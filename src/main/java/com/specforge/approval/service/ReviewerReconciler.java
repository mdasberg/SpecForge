package com.specforge.approval.service;

import com.specforge.approval.entity.RequiredReviewerEntity;
import com.specforge.approval.entity.ReviewerOrigin;
import com.specforge.approval.repository.RequiredReviewerRepository;
import com.specforge.repository.ApprovalRule;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Keeps a review's {@code RULE}-origin seats in step with its project's current rule: one seat per
 * required role, plus enough plain seats to reach {@code minApprovals}. Runs on every read and every
 * verdict, which is what makes editing the rule take effect on an open review immediately rather
 * than only on reviews opened after the edit — there is no separate "apply the new rule" step.
 *
 * <p>Only ever adds or removes an <em>unclaimed</em> seat. A claimed one is never deleted here, even
 * if the rule that created it no longer asks for it: the verdict behind it is real, and discarding
 * the seat would discard the evidence of it for no reason — the rule's own count and role check
 * decide satisfaction from what is live, so a leftover satisfied seat the current rule does not
 * strictly require is harmless.
 */
@RequiredArgsConstructor
@Component
class ReviewerReconciler {

    private final RequiredReviewerRepository reviewers;
    private final Clock clock;

    void reconcile(final UUID reviewId, final ApprovalRule rule) {
        final List<RequiredReviewerEntity> ruleSeats =
                reviewers.findByReviewIdAndOriginAndRemovedAtIsNull(reviewId, ReviewerOrigin.RULE);
        final Instant now = clock.instant();

        final Set<String> requiredRoles = new LinkedHashSet<>(rule.requiredRoles());
        final Set<String> representedRoles = new LinkedHashSet<>();
        for (final RequiredReviewerEntity seat : ruleSeats) {
            if (seat.requiredRole() != null) {
                if (requiredRoles.contains(seat.requiredRole())) {
                    representedRoles.add(seat.requiredRole());
                } else if (seat.subjectId() == null) {
                    // The rule dropped this role and nobody has claimed the seat: it is no longer
                    // asked for, so it should not sit there looking like an unmet requirement.
                    seat.remove(null, now);
                    reviewers.save(seat);
                }
            }
        }
        for (final String role : requiredRoles) {
            if (!representedRoles.contains(role)) {
                reviewers.save(RequiredReviewerEntity.ruleSlot(reviewId, role, now));
            }
        }

        final int target = Math.max(rule.minApprovals(), requiredRoles.size());
        final List<RequiredReviewerEntity> live =
                reviewers.findByReviewIdAndOriginAndRemovedAtIsNull(reviewId, ReviewerOrigin.RULE);
        final long deficit = target - live.size();
        for (long i = 0; i < deficit; i++) {
            reviewers.save(RequiredReviewerEntity.ruleSlot(reviewId, null, now));
        }
        long excess = live.size() + Math.max(deficit, 0) - target;
        if (excess > 0) {
            for (final RequiredReviewerEntity seat : live) {
                if (excess == 0) {
                    break;
                }
                if (seat.requiredRole() == null && seat.subjectId() == null) {
                    seat.remove(null, now);
                    reviewers.save(seat);
                    excess--;
                }
            }
        }
    }
}
