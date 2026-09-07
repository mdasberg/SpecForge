package com.specforge.discussion.entity;

import com.specforge.platform.ActorKind;
import com.specforge.review.AnchorState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A comment thread anchored to one section — optionally to a text range within it — of the review
 * head it was opened against.
 *
 * <p>{@code blocking} is derived from the opener's actor kind rather than stored as a choice a
 * reviewer makes: agents never approve, so a thread an agent opened cannot be what an approval is
 * waiting on, and a thread a reviewer opened always is. There is deliberately no third state.
 */
@Entity
@Table(name = "thread")
public class ThreadEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    // Plain UUIDs rather than associations: review and catalog own these rows, and a mapping here
    // would be a second way to write them.
    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "section_key", nullable = false, length = 128)
    private String sectionKey;

    @Column(name = "range_start")
    private Integer rangeStart;

    @Column(name = "range_end")
    private Integer rangeEnd;

    @Column(name = "quoted_text", nullable = false, columnDefinition = "TEXT")
    private String quotedText;

    /** The review head's content sha at the moment this thread was opened. */
    @Column(name = "anchor_content_sha", nullable = false, length = 64)
    private String anchorContentSha;

    /** The label ({@code ReviewHead.label()}) shown for that head at the same moment. */
    @Column(name = "anchor_version_label", nullable = false, length = 64)
    private String anchorVersionLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "anchor_state", nullable = false, length = 16)
    private AnchorState anchorState;

    @Column(name = "opened_by", nullable = false, length = 64)
    private String openedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "opener_actor_kind", nullable = false, length = 16)
    private ActorKind openerActorKind;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_by", length = 64)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "reopened_by", length = 64)
    private String reopenedBy;

    @Column(name = "reopened_at")
    private Instant reopenedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ThreadEntity() {
        // for JPA
    }

    public ThreadEntity(
            final UUID id,
            final UUID reviewId,
            final UUID documentId,
            final String sectionKey,
            final Integer rangeStart,
            final Integer rangeEnd,
            final String quotedText,
            final String anchorContentSha,
            final String anchorVersionLabel,
            final String openedBy,
            final ActorKind openerActorKind,
            final Instant now) {
        this.id = id;
        this.reviewId = reviewId;
        this.documentId = documentId;
        this.sectionKey = sectionKey;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.quotedText = quotedText;
        this.anchorContentSha = anchorContentSha;
        this.anchorVersionLabel = anchorVersionLabel;
        this.anchorState = AnchorState.CURRENT;
        this.openedBy = openedBy;
        this.openerActorKind = openerActorKind;
        this.resolved = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void resolve(final String actor, final Instant now) {
        this.resolved = true;
        this.resolvedBy = actor;
        this.resolvedAt = now;
        this.updatedAt = now;
    }

    public void reopen(final String actor, final Instant now) {
        this.resolved = false;
        this.reopenedBy = actor;
        this.reopenedAt = now;
        this.updatedAt = now;
    }

    public void carry(final AnchorState state, final Instant now) {
        this.anchorState = state;
        this.updatedAt = now;
    }

    public void touch(final Instant now) {
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public UUID documentId() {
        return documentId;
    }

    public String sectionKey() {
        return sectionKey;
    }

    public Integer rangeStart() {
        return rangeStart;
    }

    public Integer rangeEnd() {
        return rangeEnd;
    }

    public String quotedText() {
        return quotedText;
    }

    public String anchorContentSha() {
        return anchorContentSha;
    }

    public String anchorVersionLabel() {
        return anchorVersionLabel;
    }

    public AnchorState anchorState() {
        return anchorState;
    }

    public String openedBy() {
        return openedBy;
    }

    public ActorKind openerActorKind() {
        return openerActorKind;
    }

    /** Agents never approve, so only a human-opened thread can be what an approval waits on. */
    public boolean blocking() {
        return openerActorKind == ActorKind.HUMAN;
    }

    public boolean resolved() {
        return resolved;
    }

    public String resolvedBy() {
        return resolvedBy;
    }

    public Instant resolvedAt() {
        return resolvedAt;
    }

    public String reopenedBy() {
        return reopenedBy;
    }

    public Instant reopenedAt() {
        return reopenedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
