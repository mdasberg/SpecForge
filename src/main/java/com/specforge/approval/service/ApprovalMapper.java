package com.specforge.approval.service;

import com.specforge.approval.CheckState;
import com.specforge.approval.entity.RequiredReviewerEntity;
import com.specforge.approval.entity.VerdictEntity;
import com.specforge.platform.ActorKind;
import com.specforge.platform.MemberRef;
import com.specforge.platform.api.dto.ActorRef;
import com.specforge.platform.api.dto.ApprovalGateState;
import com.specforge.platform.api.dto.ApprovalRuleState;
import com.specforge.platform.api.dto.ApprovalStatus;
import com.specforge.platform.api.dto.ChecksSummary;
import com.specforge.platform.api.dto.ComposerState;
import com.specforge.platform.api.dto.RequiredReviewer;
import com.specforge.platform.api.dto.Verdict;
import com.specforge.platform.api.dto.VerdictType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Entity to contract. It is the only place the approval panel's wire shape is decided. */
@Component
class ApprovalMapper {

    ApprovalStatus status(
            final java.util.UUID reviewId,
            final List<RequiredReviewerEntity> reviewers,
            final List<VerdictEntity> verdicts,
            final ApprovalGate.Result gate,
            final int unresolvedBlockingThreads,
            final ComposerState composer,
            final Map<String, MemberRef> actors) {
        return new ApprovalStatus(
                reviewId,
                reviewers.stream().map(r -> reviewer(r, actors)).toList(),
                ruleState(gate.rule()),
                unresolvedBlockingThreads,
                checks(gate.checks()),
                gateState(gate),
                verdicts.stream().map(v -> verdict(v, actors)).toList(),
                composer);
    }

    private RequiredReviewer reviewer(final RequiredReviewerEntity entity, final Map<String, MemberRef> actors) {
        final RequiredReviewer dto = new RequiredReviewer(
                entity.id(),
                RequiredReviewer.OriginEnum.fromValue(entity.origin().name()),
                RequiredReviewer.StateEnum.fromValue(entity.state().name()));
        dto.setRequiredRole(entity.requiredRole());
        if (entity.subjectId() != null) {
            dto.setReviewer(ref(entity.subjectId(), actors));
        }
        if (entity.addedBy() != null) {
            dto.setAddedBy(ref(entity.addedBy(), actors));
            dto.setAddedAt(at(entity.addedAt()));
        }
        return dto;
    }

    private Verdict verdict(final VerdictEntity entity, final Map<String, MemberRef> actors) {
        final Verdict dto = new Verdict(
                entity.id(),
                ref(entity.subjectId(), entity.actorKind(), actors),
                VerdictType.fromValue(entity.verdictType().name()),
                entity.headLabel(),
                at(entity.createdAt()));
        dto.setBody(entity.body());
        return dto;
    }

    private static ApprovalRuleState ruleState(final ApprovalGate.RuleState rule) {
        return new ApprovalRuleState(rule.satisfied(), rule.approvedCount(), rule.requiredCount(), rule.reason());
    }

    private static ChecksSummary checks(final CheckState checks) {
        return new ChecksSummary(checks.configured(), checks.failed(), checks.failedCheckNames());
    }

    private static ApprovalGateState gateState(final ApprovalGate.Result gate) {
        return new ApprovalGateState(gate.passed(), gate.reasons());
    }

    /** Resolves through the identity mirror when it can, and falls back to what the row itself knows. */
    private static ActorRef ref(final String subjectId, final ActorKind actorKind, final Map<String, MemberRef> actors) {
        final MemberRef member = actors.get(subjectId);
        return member == null
                ? new ActorRef(subjectId, subjectId, dto(actorKind))
                : new ActorRef(member.subjectId(), member.displayName(), dto(member.actorKind()))
                        .handle(member.handle())
                        .avatarUrl(member.avatarUrl());
    }

    private static ActorRef ref(final String subjectId, final Map<String, MemberRef> actors) {
        final MemberRef member = actors.get(subjectId);
        return member == null
                ? new ActorRef(subjectId, subjectId, dto(ActorKind.HUMAN))
                : new ActorRef(member.subjectId(), member.displayName(), dto(member.actorKind()))
                        .handle(member.handle())
                        .avatarUrl(member.avatarUrl());
    }

    private static com.specforge.platform.api.dto.ActorKind dto(final ActorKind actorKind) {
        return com.specforge.platform.api.dto.ActorKind.fromValue(actorKind.name());
    }

    private static OffsetDateTime at(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
