package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.DispositionKind;
import com.specforge.agent.entity.FindingDispositionEntity;
import com.specforge.agent.entity.FindingEntity;
import com.specforge.agent.repository.CheckRunRepository;
import com.specforge.agent.repository.FindingDispositionRepository;
import com.specforge.agent.repository.FindingRepository;
import com.specforge.discussion.Discussions;
import com.specforge.platform.ActorKind;
import com.specforge.platform.Caller;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.DiscussFindingRequest;
import com.specforge.platform.api.dto.Finding;
import com.specforge.review.Reviews;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a human does with a finding: accept it, dismiss it, argue with it, or change their mind.
 *
 * <p>Accepting opens a thread carrying the proposed text and changes no specification content — in
 * SpecForge or in the repository, which SpecForge only ever reads. "Apply" would be a lie, and the
 * thread is the honest version of it: a reviewer decided this is worth doing, and now it is on the
 * record where the change actually has to be made.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class FindingService {

    private final FindingRepository findings;
    private final FindingDispositionRepository dispositions;
    private final CheckRunRepository runs;
    private final CheckCatalogue catalogue;
    private final Discussions discussions;
    private final Reviews reviews;
    private final Members members;
    private final AgentMapper mapper;
    private final Clock clock;

    public Finding accept(final UUID findingId, final Caller caller) {
        final FindingEntity finding = require(findingId);
        final Context context = context(finding, caller);
        requireUndisposed(finding);

        final UUID threadId = discussions.openOnSection(
                finding.reviewId(), finding.sectionKey(), acceptBody(finding, context), caller);
        dispose(finding, DispositionKind.ACCEPTED, caller, threadId);
        return view(finding, context);
    }

    public Finding dismiss(final UUID findingId, final Caller caller) {
        final FindingEntity finding = require(findingId);
        final Context context = context(finding, caller);
        requireUndisposed(finding);

        // No justification is asked for on purpose: requiring one teaches reviewers to type "n/a",
        // and who dismissed what, reversibly, is the accountability that actually holds.
        dispose(finding, DispositionKind.DISMISSED, caller, null);
        return view(finding, context);
    }

    public Finding discuss(final UUID findingId, final DiscussFindingRequest request, final Caller caller) {
        final FindingEntity finding = require(findingId);
        final Context context = context(finding, caller);

        discussions.openOnSection(finding.reviewId(), finding.sectionKey(), request.getBody(), caller);
        // Deliberately no disposition: arguing with a finding is not deciding about it, and the
        // finding stays on the active list until somebody does decide.
        return view(finding, context);
    }

    public Finding undo(final UUID findingId, final Caller caller) {
        final FindingEntity finding = require(findingId);
        final Context context = context(finding, caller);
        final FindingDispositionEntity current = current(findingId);
        if (current == null || current.kind() == DispositionKind.UNDONE) {
            throw Problems.conflict("Finding %s has no disposition to undo.".formatted(findingId));
        }

        // The undo is a row of its own; the thread an accept opened is left where it is, because a
        // conversation other people may have joined is not SpecForge's to delete.
        dispose(finding, DispositionKind.UNDONE, caller, null);
        return view(finding, context);
    }

    private void dispose(
            final FindingEntity finding, final DispositionKind kind, final Caller caller, final UUID threadId) {
        final Instant now = clock.instant();
        dispositions.save(new FindingDispositionEntity(
                UUID.randomUUID(), finding.id(), kind, caller.subjectId(), threadId, now));
    }

    private static String acceptBody(final FindingEntity finding, final Context context) {
        final StringBuilder body = new StringBuilder("Accepting **%s**, found by %s (`%s`).\n\n%s".formatted(
                finding.title(),
                context.nameOf(finding.authorSubjectId()),
                context.definition().checkKey(),
                quote(finding.body())));
        if (finding.proposedText() != null && !finding.proposedText().isBlank()) {
            body.append("\n\nProposed:\n\n```\n").append(finding.proposedText()).append("\n```");
        }
        return body.toString();
    }

    private static String quote(final String body) {
        return body.lines().map(line -> "> " + line).reduce((a, b) -> a + "\n" + b).orElse("> ");
    }

    private void requireUndisposed(final FindingEntity finding) {
        final FindingDispositionEntity current = current(finding.id());
        if (current != null && current.kind() != DispositionKind.UNDONE) {
            throw Problems.conflict("Finding %s is already %s; undo that first.".formatted(
                    finding.id(), current.kind().name().toLowerCase(java.util.Locale.ROOT)));
        }
    }

    private FindingDispositionEntity current(final UUID findingId) {
        final List<FindingDispositionEntity> history = dispositions.findByFindingIdInOrderByCreatedAtAsc(
                List.of(findingId));
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    /**
     * The guards every disposition shares: a closed review is finished with, and an agent identity
     * disposes of nothing — a check runner clearing its own findings would make the check a formality.
     */
    private Context context(final FindingEntity finding, final Caller caller) {
        if (caller.actorKind() != ActorKind.HUMAN) {
            throw Problems.conflict("An agent identity may not accept, dismiss or discuss a finding.");
        }
        if (!reviews.ref(finding.reviewId())
                .orElseThrow(() -> Problems.notFound("No review %s.".formatted(finding.reviewId())))
                .open()) {
            throw Problems.conflict("Review %s is closed.".formatted(finding.reviewId()));
        }
        final CheckRunEntity run = runs.findById(finding.checkRunId())
                .orElseThrow(() -> Problems.conflict("The run behind finding %s is gone.".formatted(finding.id())));
        final CheckDefinitionEntity definition = catalogue.definition(run.checkDefinitionId())
                .orElseThrow(() -> Problems.conflict(
                        "The check behind finding %s is no longer configured.".formatted(finding.id())));
        final Map<String, MemberRef> actors = members.byIds(Set.of(finding.authorSubjectId(), caller.subjectId()));
        return new Context(run, definition, actors);
    }

    private Finding view(final FindingEntity finding, final Context context) {
        return mapper.finding(finding, context.definition(), current(finding.id()), context.run(), context.actors());
    }

    private FindingEntity require(final UUID findingId) {
        return findings.findById(findingId)
                .orElseThrow(() -> Problems.notFound("No finding %s.".formatted(findingId)));
    }

    private record Context(CheckRunEntity run, CheckDefinitionEntity definition, Map<String, MemberRef> actors) {

        /** The agent's own name, so the thread says who suggested this rather than "an agent". */
        String nameOf(final String subjectId) {
            final MemberRef agent = actors.get(subjectId);
            return agent == null ? "an automated reviewer" : agent.displayName();
        }
    }
}
