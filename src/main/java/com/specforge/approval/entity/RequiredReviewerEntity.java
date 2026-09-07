package com.specforge.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * One seat the approval rule requires filled before a review can pass the gate.
 *
 * <p>A {@code RULE}-origin row starts unclaimed ({@code subjectId} null): the reconciler creates one
 * per required role the project names, plus enough plain ones to reach {@code minApprovals}, and
 * keeps that set in step with the rule every time the review is read — which is what makes editing
 * the rule take effect on an open review immediately, rather than only on the reviews opened after
 * the edit. Casting a qualifying verdict claims the first compatible unclaimed row (an exact-role
 * match before a plain one), binding {@code subjectId}; a verdict that cannot claim any row is
 * still recorded, just optionally — it never fills a seat, so it is exactly what the "optional
 * reviewer" scenario means.
 *
 * <p>A {@code MANUAL} row is added by an administrator naming a specific person on one review, on
 * top of whatever the rule already requires. The reconciler never creates, claims or removes one:
 * only an administrator does, which is what makes it survive a rule edit and a head advance alike.
 *
 * <p>Removal is a soft delete: the person the row was about may have left the team, but the verdict
 * they cast against an earlier head stays in {@code review_verdict}'s history regardless.
 */
@Entity
@Table(name = "review_reviewer")
public class RequiredReviewerEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", nullable = false, length = 16)
    private ReviewerOrigin origin;

    /** Set only while a {@code RULE} row is unclaimed; irrelevant once {@code subjectId} is bound. */
    @Column(name = "required_role", length = 32)
    private String requiredRole;

    @Column(name = "subject_id", length = 64)
    private String subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private ReviewerState state;

    @Column(name = "added_by", length = 64)
    private String addedBy;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "removed_by", length = 64)
    private String removedBy;

    @Column(name = "removed_at")
    private Instant removedAt;

    protected RequiredReviewerEntity() {
        // for JPA
    }

    /** A rule-derived seat, unclaimed until a compatible verdict is cast. */
    public static RequiredReviewerEntity ruleSlot(final UUID reviewId, final String requiredRole, final Instant now) {
        final RequiredReviewerEntity entity = new RequiredReviewerEntity();
        entity.id = UUID.randomUUID();
        entity.reviewId = reviewId;
        entity.origin = ReviewerOrigin.RULE;
        entity.requiredRole = requiredRole;
        entity.state = ReviewerState.PENDING;
        entity.addedAt = now;
        entity.updatedAt = now;
        return entity;
    }

    /** An administrator naming a specific person as required on this one review. */
    public static RequiredReviewerEntity manual(
            final UUID reviewId, final String subjectId, final String addedBy, final Instant now) {
        final RequiredReviewerEntity entity = new RequiredReviewerEntity();
        entity.id = UUID.randomUUID();
        entity.reviewId = reviewId;
        entity.origin = ReviewerOrigin.MANUAL;
        entity.subjectId = subjectId;
        entity.state = ReviewerState.PENDING;
        entity.addedBy = addedBy;
        entity.addedAt = now;
        entity.updatedAt = now;
        return entity;
    }

    /** Whether {@code caller} may claim this seat with a verdict: already theirs, or open to their role. */
    public boolean claimableBy(final String callerSubjectId, final Set<String> callerRoles) {
        if (removedAt != null) {
            return false;
        }
        if (subjectId != null) {
            return subjectId.equals(callerSubjectId);
        }
        return requiredRole == null || callerRoles.contains(requiredRole);
    }

    public void claim(final String callerSubjectId, final Instant now) {
        this.subjectId = callerSubjectId;
        this.updatedAt = now;
    }

    public void approve(final Instant now) {
        this.state = ReviewerState.APPROVED;
        this.updatedAt = now;
    }

    public void requestChanges(final Instant now) {
        this.state = ReviewerState.CHANGES_REQUESTED;
        this.updatedAt = now;
    }

    /** A new head arrived: whatever verdict this seat held no longer speaks for it. */
    public void resetToPending(final Instant now) {
        this.state = ReviewerState.PENDING;
        this.updatedAt = now;
    }

    public void remove(final String actor, final Instant now) {
        this.removedBy = actor;
        this.removedAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public ReviewerOrigin origin() {
        return origin;
    }

    public String requiredRole() {
        return requiredRole;
    }

    public String subjectId() {
        return subjectId;
    }

    public ReviewerState state() {
        return state;
    }

    public String addedBy() {
        return addedBy;
    }

    public Instant addedAt() {
        return addedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String removedBy() {
        return removedBy;
    }

    public Instant removedAt() {
        return removedAt;
    }

    public boolean removed() {
        return removedAt != null;
    }
}
