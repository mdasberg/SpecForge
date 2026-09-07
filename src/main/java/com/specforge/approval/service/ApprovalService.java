package com.specforge.approval.service;

import com.specforge.approval.BlockingChecks;
import com.specforge.approval.CheckState;
import com.specforge.approval.entity.RequiredReviewerEntity;
import com.specforge.approval.entity.ReviewerState;
import com.specforge.approval.entity.VerdictEntity;
import com.specforge.approval.entity.VerdictType;
import com.specforge.approval.repository.RequiredReviewerRepository;
import com.specforge.approval.repository.VerdictRepository;
import com.specforge.catalog.SpecCatalog;
import com.specforge.discussion.Discussions;
import com.specforge.platform.ActorKind;
import com.specforge.platform.Caller;
import com.specforge.platform.MemberRef;
import com.specforge.platform.Members;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.AddRequiredReviewerRequest;
import com.specforge.platform.api.dto.ApprovalStatus;
import com.specforge.platform.api.dto.ComposerState;
import com.specforge.platform.api.dto.VerdictRequest;
import com.specforge.repository.ApprovalRule;
import com.specforge.repository.ApprovalRules;
import com.specforge.repository.ReviewOutcome;
import com.specforge.repository.ReviewStatusReporter;
import com.specforge.review.ReviewHead;
import com.specforge.review.ReviewHeadAdvanced;
import com.specforge.review.ReviewRef;
import com.specforge.review.Reviews;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Required reviewers, verdicts and the gate they feed.
 *
 * <p>Every public method here writes, even the ones that only look like a read: the seat set is
 * reconciled against the project's live rule first, which is what makes an edited rule take effect
 * on an open review immediately. There is deliberately no separate {@code readOnly} query path.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class ApprovalService {

    private final RequiredReviewerRepository reviewers;
    private final VerdictRepository verdicts;
    private final ReviewerReconciler reconciler;
    private final Reviews reviewsPort;
    private final SpecCatalog catalog;
    private final ApprovalRules approvalRules;
    private final ReviewStatusReporter reviewStatus;
    private final Discussions discussions;
    private final BlockingChecks blockingChecks;
    private final Members members;
    private final ApprovalMapper mapper;
    private final Clock clock;

    public ApprovalStatus status(final UUID reviewId, final Caller caller) {
        final ReviewRef ref = requireReview(reviewId);
        return buildStatus(reviewId, ref, caller);
    }

    public ApprovalStatus castVerdict(final UUID reviewId, final VerdictRequest request, final Caller caller) {
        final ReviewRef ref = requireOpenReview(reviewId);
        final ReviewHead head = reviewsPort.head(reviewId)
                .orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
        if (!head.contentSha().equals(request.getAtHeadSha())) {
            throw Problems.conflict(
                    "The review's head advanced to %s since this verdict was prepared.".formatted(head.label()));
        }
        final VerdictType type = VerdictType.valueOf(request.getVerdictType().name());
        if (caller.actorKind() == ActorKind.AGENT && type != VerdictType.COMMENT) {
            throw Problems.conflict("An agent identity may not approve or request changes.");
        }

        final Instant now = clock.instant();
        upsertVerdict(reviewId, caller, type, request.getBody(), head, now);

        if (type != VerdictType.COMMENT) {
            final ApprovalRule rule = ruleFor(ref.documentId());
            reconciler.reconcile(reviewId, rule);
            final List<RequiredReviewerEntity> seats = liveSeats(reviewId);
            final RequiredReviewerEntity claimed = claim(seats, caller, type, now);
            if (claimed != null) {
                if (type == VerdictType.REQUEST_CHANGES) {
                    catalog.requestChanges(ref.documentId());
                    reportOutcome(ref, ReviewOutcome.CHANGES_REQUESTED, "A required reviewer requested changes.");
                } else {
                    final ApprovalGate.Result gate = evaluateGate(reviewId, seats, rule);
                    if (gate.passed()) {
                        catalog.approve(ref.documentId());
                        reportOutcome(ref, ReviewOutcome.APPROVED, gate.rule().reason());
                    }
                }
            }
        }
        return buildStatus(reviewId, ref, caller);
    }

    public ApprovalStatus addRequiredReviewer(
            final UUID reviewId, final AddRequiredReviewerRequest request, final Caller caller) {
        final ReviewRef ref = requireOpenReview(reviewId);
        reviewers.save(RequiredReviewerEntity.manual(reviewId, request.getSubjectId(), caller.subjectId(), clock.instant()));
        return buildStatus(reviewId, ref, caller);
    }

    public ApprovalStatus removeRequiredReviewer(final UUID reviewId, final UUID requiredReviewerId, final Caller caller) {
        final ReviewRef ref = requireOpenReview(reviewId);
        final RequiredReviewerEntity seat = reviewers.findById(requiredReviewerId)
                .filter(candidate -> candidate.reviewId().equals(reviewId) && !candidate.removed())
                .orElseThrow(() -> Problems.notFound(
                        "No required reviewer %s on review %s.".formatted(requiredReviewerId, reviewId)));
        seat.remove(caller.subjectId(), clock.instant());
        reviewers.save(seat);
        return buildStatus(reviewId, ref, caller);
    }

    /** A new head arrived: nothing any seat holds still speaks for it. */
    public void onHeadAdvanced(final ReviewHeadAdvanced event) {
        final Instant now = clock.instant();
        for (final RequiredReviewerEntity seat : reviewers.findByReviewIdAndRemovedAtIsNull(event.reviewId())) {
            if (seat.state() != ReviewerState.PENDING) {
                seat.resetToPending(now);
                reviewers.save(seat);
            }
        }
    }

    private ApprovalStatus buildStatus(final UUID reviewId, final ReviewRef ref, final Caller caller) {
        final ApprovalRule rule = ruleFor(ref.documentId());
        reconciler.reconcile(reviewId, rule);
        final List<RequiredReviewerEntity> seats = liveSeats(reviewId);
        final ApprovalGate.Result gate = evaluateGate(reviewId, seats, rule);
        final String headContentSha = reviewsPort.head(reviewId)
                .map(ReviewHead::contentSha)
                .orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
        final List<VerdictEntity> log = verdicts.findByReviewIdAndHeadContentShaOrderByCreatedAtDesc(reviewId, headContentSha);
        final ComposerState composer = composerFor(caller, ref);
        final Map<String, MemberRef> actors = resolveActors(seats, log);
        return mapper.status(reviewId, seats, log, gate, gate.unresolvedBlockingThreads(), composer, actors);
    }

    private RequiredReviewerEntity claim(
            final List<RequiredReviewerEntity> seats, final Caller caller, final VerdictType type, final Instant now) {
        RequiredReviewerEntity seat = seats.stream()
                .filter(candidate -> caller.subjectId().equals(candidate.subjectId()))
                .findFirst()
                .orElseGet(() -> unclaimedFor(seats, caller));
        if (seat == null) {
            return null;
        }
        if (seat.subjectId() == null) {
            seat.claim(caller.subjectId(), now);
        }
        if (type == VerdictType.APPROVE) {
            seat.approve(now);
        } else {
            seat.requestChanges(now);
        }
        reviewers.save(seat);
        return seat;
    }

    /** An exact role match is offered before a plain seat, so a role requirement is filled first. */
    private static RequiredReviewerEntity unclaimedFor(final List<RequiredReviewerEntity> seats, final Caller caller) {
        return seats.stream()
                .filter(candidate -> candidate.subjectId() == null
                        && candidate.claimableBy(caller.subjectId(), caller.roles()))
                .sorted(Comparator.comparing(candidate -> candidate.requiredRole() == null))
                .findFirst()
                .orElse(null);
    }

    private void upsertVerdict(
            final UUID reviewId, final Caller caller, final VerdictType type, final String body,
            final ReviewHead head, final Instant now) {
        verdicts.findByReviewIdAndSubjectIdAndHeadContentSha(reviewId, caller.subjectId(), head.contentSha())
                .map(existing -> {
                    existing.recast(type, body, now);
                    return verdicts.save(existing);
                })
                .orElseGet(() -> verdicts.save(new VerdictEntity(
                        UUID.randomUUID(), reviewId, caller.subjectId(), caller.actorKind(), type, body,
                        head.contentSha(), head.label(), now)));
    }

    private ApprovalGate.Result evaluateGate(
            final UUID reviewId, final List<RequiredReviewerEntity> seats, final ApprovalRule rule) {
        final int unresolvedBlocking = discussions.unresolvedBlockingCount(reviewId);
        final CheckState checkState = blockingChecks.state(reviewId);
        return ApprovalGate.gate(seats, rule, unresolvedBlocking, checkState);
    }

    private ApprovalRule ruleFor(final UUID documentId) {
        final UUID connectionId = catalog.locate(documentId)
                .orElseThrow(() -> new IllegalStateException("No location for specification %s.".formatted(documentId)))
                .connectionId();
        return approvalRules.forConnection(connectionId)
                .orElseThrow(() -> new IllegalStateException("No approval rule behind connection %s.".formatted(connectionId)));
    }

    private void reportOutcome(final ReviewRef ref, final ReviewOutcome outcome, final String description) {
        if (ref.proposalId() != null) {
            reviewStatus.report(ref.proposalId(), outcome, description);
        }
    }

    private static ComposerState composerFor(final Caller caller, final ReviewRef ref) {
        final boolean human = caller.actorKind() == ActorKind.HUMAN;
        String disabledReason = null;
        if (!ref.open()) {
            disabledReason = "This review is closed.";
        } else if (!human) {
            disabledReason = "Agents may comment, but may not approve or request changes.";
        }
        final boolean canVote = ref.open() && human;
        final ComposerState composer = new ComposerState(canVote, canVote, ref.open());
        composer.setDisabledReason(disabledReason);
        return composer;
    }

    private List<RequiredReviewerEntity> liveSeats(final UUID reviewId) {
        return reviewers.findByReviewIdAndRemovedAtIsNullOrderByAddedAtAsc(reviewId);
    }

    private ReviewRef requireReview(final UUID reviewId) {
        return reviewsPort.ref(reviewId).orElseThrow(() -> Problems.notFound("No review %s.".formatted(reviewId)));
    }

    private ReviewRef requireOpenReview(final UUID reviewId) {
        final ReviewRef ref = requireReview(reviewId);
        if (!ref.open()) {
            throw Problems.conflict("Review %s is closed.".formatted(reviewId));
        }
        return ref;
    }

    private Map<String, MemberRef> resolveActors(final List<RequiredReviewerEntity> seats, final List<VerdictEntity> log) {
        final Set<String> subjectIds = new LinkedHashSet<>();
        for (final RequiredReviewerEntity seat : seats) {
            if (seat.subjectId() != null) {
                subjectIds.add(seat.subjectId());
            }
            if (seat.addedBy() != null) {
                subjectIds.add(seat.addedBy());
            }
        }
        for (final VerdictEntity verdict : log) {
            subjectIds.add(verdict.subjectId());
        }
        return members.byIds(subjectIds);
    }
}
