package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.FindingDispositionEntity;
import com.specforge.agent.entity.FindingEntity;
import com.specforge.agent.repository.CheckRunRepository;
import com.specforge.agent.repository.FindingDispositionRepository;
import com.specforge.agent.repository.FindingRepository;
import com.specforge.catalog.SpecCatalog;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.CheckRun;
import com.specforge.platform.api.dto.CheckRunList;
import com.specforge.platform.api.dto.ChecksSummary;
import com.specforge.repository.ProjectRef;
import com.specforge.repository.Projects;
import com.specforge.review.ReviewRef;
import com.specforge.review.Reviews;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Checks tab and the reruns behind it.
 *
 * <p>Every read here goes through the live runs — the ones not yet superseded — because the tab is
 * a statement about the head under review. The stale ones are not deleted and stay readable through
 * the findings and dispositions that still point at them, which is what makes "this finding is new
 * since the last push" answerable at all.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class CheckService {

    private final CheckRunRepository runs;
    private final FindingRepository findings;
    private final FindingDispositionRepository dispositions;
    private final CheckCatalogue catalogue;
    private final CheckDispatcher dispatcher;
    private final Reviews reviews;
    private final SpecCatalog catalog;
    private final Projects projects;
    private final Members members;
    private final AgentMapper mapper;

    public CheckRunList list(final UUID reviewId) {
        requireReview(reviewId);
        return checkRunList(reviewId);
    }

    /** Every enabled check again, on the current head. The previous runs are kept and marked stale. */
    public CheckRunList rerunAll(final UUID reviewId) {
        requireOpenReview(reviewId);
        dispatcher.dispatchAll(reviewId);
        return checkRunList(reviewId);
    }

    /** One check again — the same act as rerunning all of them, narrowed to a single definition. */
    public CheckRunList rerun(final UUID checkRunId) {
        final CheckRunEntity run = runs.findById(checkRunId)
                .orElseThrow(() -> Problems.notFound("No check run %s.".formatted(checkRunId)));
        requireOpenReview(run.reviewId());
        dispatcher.dispatchOne(checkRunId);
        return checkRunList(run.reviewId());
    }

    /** The counted live runs, for the approval gate and for the summary line on the panel. */
    CheckCounts counts(final UUID reviewId) {
        final UUID projectId = projectId(reviewId);
        if (projectId == null || catalogue.enabledForProject(projectId).isEmpty()) {
            return CheckCounts.unconfigured();
        }
        final List<CheckRunEntity> live = runs.findByReviewIdAndStaleFalseOrderByQueuedAtAsc(reviewId);
        return CheckCounts.of(true, live, definitionsFor(live));
    }

    private CheckRunList checkRunList(final UUID reviewId) {
        final List<CheckRunEntity> live = runs.findByReviewIdAndStaleFalseOrderByQueuedAtAsc(reviewId);
        final Map<UUID, CheckDefinitionEntity> definitions = definitionsFor(live);
        final List<FindingEntity> allFindings = live.isEmpty()
                ? List.of()
                : findings.findByCheckRunIdInOrderByCreatedAtAsc(live.stream().map(CheckRunEntity::id).toList());
        final Map<UUID, List<FindingEntity>> byRun = allFindings.stream()
                .collect(Collectors.groupingBy(FindingEntity::checkRunId, LinkedHashMap::new, Collectors.toList()));
        final Map<UUID, FindingDispositionEntity> current = currentDispositions(allFindings);
        final Map<String, MemberRef> actors = resolveActors(allFindings, current.values());

        final List<CheckRun> items = new ArrayList<>(live.size());
        for (final CheckRunEntity run : live) {
            final CheckDefinitionEntity definition = definitions.get(run.checkDefinitionId());
            if (definition != null) {
                items.add(mapper.run(run, definition, byRun.getOrDefault(run.id(), List.of()), current, actors));
            }
        }
        final CheckCounts counts = counts(reviewId);
        return new CheckRunList(items, new ChecksSummary(
                counts.configured(),
                counts.blockingFailure(),
                counts.failedBlockingNames(),
                counts.total(),
                counts.passed(),
                counts.failed(),
                counts.pending()));
    }

    /** A finding's disposition is its latest row; an undo leaves the finding active again. */
    Map<UUID, FindingDispositionEntity> currentDispositions(final List<FindingEntity> forFindings) {
        if (forFindings.isEmpty()) {
            return Map.of();
        }
        final Map<UUID, FindingDispositionEntity> latest = new LinkedHashMap<>();
        for (final FindingDispositionEntity disposition : dispositions.findByFindingIdInOrderByCreatedAtAsc(
                forFindings.stream().map(FindingEntity::id).toList())) {
            latest.put(disposition.findingId(), disposition);
        }
        return latest;
    }

    private Map<UUID, CheckDefinitionEntity> definitionsFor(final List<CheckRunEntity> live) {
        return live.isEmpty()
                ? Map.of()
                : catalogue.definitions(live.stream().map(CheckRunEntity::checkDefinitionId).toList());
    }

    private Map<String, MemberRef> resolveActors(
            final List<FindingEntity> findingRows, final Iterable<FindingDispositionEntity> dispositionRows) {
        final Set<String> subjectIds = new LinkedHashSet<>();
        findingRows.forEach(finding -> subjectIds.add(finding.authorSubjectId()));
        dispositionRows.forEach(disposition -> subjectIds.add(disposition.actorSubjectId()));
        return subjectIds.isEmpty() ? Map.of() : members.byIds(subjectIds);
    }

    private UUID projectId(final UUID reviewId) {
        return reviews.ref(reviewId)
                .flatMap(ref -> catalog.locate(ref.documentId()))
                .flatMap(location -> projects.forConnection(location.connectionId()))
                .map(ProjectRef::projectId)
                .orElse(null);
    }

    private ReviewRef requireReview(final UUID reviewId) {
        return reviews.ref(reviewId).orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
    }

    private void requireOpenReview(final UUID reviewId) {
        if (!requireReview(reviewId).open()) {
            throw Problems.conflict("Review %s is closed.".formatted(reviewId));
        }
    }
}
