package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.DispositionKind;
import com.specforge.agent.entity.FindingDispositionEntity;
import com.specforge.agent.entity.FindingEntity;
import com.specforge.platform.ActorKind;
import com.specforge.platform.MemberRef;
import com.specforge.platform.api.dto.ActorRef;
import com.specforge.platform.api.dto.CheckRun;
import com.specforge.platform.api.dto.CheckRunState;
import com.specforge.platform.api.dto.CheckRunnerKind;
import com.specforge.platform.api.dto.Finding;
import com.specforge.platform.api.dto.FindingDisposition;
import com.specforge.platform.api.dto.FindingDispositionKind;
import com.specforge.platform.api.dto.FindingSeverity;
import com.specforge.platform.api.dto.TextRange;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Entities to the contract's types. The display name and the blocking flag come from the definition
 * rather than from the run, so an administrator turning a check from advisory to blocking shows up
 * on the reviews that are already open — which is what the requirement asks for.
 */
@Component
class AgentMapper {

    CheckRun run(
            final CheckRunEntity run,
            final CheckDefinitionEntity definition,
            final List<FindingEntity> runFindings,
            final Map<java.util.UUID, FindingDispositionEntity> dispositions,
            final Map<String, MemberRef> actors) {
        final CheckRun dto = new CheckRun(
                run.id(),
                definition.checkKey(),
                definition.displayName(),
                definition.blocking(),
                CheckRunnerKind.fromValue(definition.runnerKind().name()),
                CheckRunState.fromValue(run.state().name()),
                run.headLabel(),
                run.stale(),
                at(run.queuedAt()),
                runFindings.stream()
                        .map(finding -> finding(finding, definition, dispositions.get(finding.id()), run, actors))
                        .toList());
        dto.setSummary(run.summary());
        dto.setModelIdentity(run.modelIdentity());
        dto.setDurationMs(run.durationMs());
        dto.setStartedAt(at(run.startedAt()));
        dto.setFinishedAt(at(run.finishedAt()));
        return dto;
    }

    Finding finding(
            final FindingEntity finding,
            final CheckDefinitionEntity definition,
            final FindingDispositionEntity disposition,
            final CheckRunEntity run,
            final Map<String, MemberRef> actors) {
        final Finding dto = new Finding(
                finding.id(),
                finding.checkRunId(),
                definition.checkKey(),
                finding.sectionKey(),
                FindingSeverity.fromValue(finding.severity().name()),
                finding.title(),
                finding.body(),
                ref(finding.authorSubjectId(), actors),
                at(finding.createdAt()));
        dto.setProposedText(finding.proposedText());
        dto.setModelIdentity(run == null ? null : run.modelIdentity());
        if (finding.rangeStart() != null && finding.rangeEnd() != null) {
            dto.setRange(new TextRange(finding.rangeStart(), finding.rangeEnd()));
        }
        if (disposition != null && disposition.kind() != DispositionKind.UNDONE) {
            final FindingDisposition current = new FindingDisposition(
                    FindingDispositionKind.fromValue(disposition.kind().name()),
                    ref(disposition.actorSubjectId(), actors),
                    at(disposition.createdAt()));
            current.setThreadId(disposition.threadId());
            dto.setDisposition(current);
        }
        return dto;
    }

    /** Resolves through the identity mirror when it can; an agent row is always mirrored, being seeded. */
    private static ActorRef ref(final String subjectId, final Map<String, MemberRef> actors) {
        final MemberRef member = actors.get(subjectId);
        if (member == null) {
            return new ActorRef(subjectId, subjectId, com.specforge.platform.api.dto.ActorKind.AGENT);
        }
        final ActorRef ref = new ActorRef(
                member.subjectId(),
                member.displayName(),
                com.specforge.platform.api.dto.ActorKind.fromValue(
                        (member.actorKind() == null ? ActorKind.AGENT : member.actorKind()).name()));
        ref.setHandle(member.handle());
        ref.setAvatarUrl(member.avatarUrl());
        return ref;
    }

    private static OffsetDateTime at(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
