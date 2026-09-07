package com.specforge.approval.entity;

import com.specforge.platform.ActorKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One cast of a verdict against one head. Recasting against the same head updates this row in
 * place — a reviewer changing their mind before anyone else has moved is one opinion, not two —
 * but a head advance never touches an existing row: it is frozen history the moment a new head
 * makes it stale, exactly like {@code required_role}'s row it may or may not have claimed.
 */
@Entity
@Table(name = "review_verdict")
public class VerdictEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "subject_id", nullable = false, length = 64)
    private String subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_kind", nullable = false, length = 16)
    private ActorKind actorKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_type", nullable = false, length = 32)
    private VerdictType verdictType;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Column(name = "head_content_sha", nullable = false, length = 64)
    private String headContentSha;

    /** Snapshotted at cast time, the same as {@code ThreadEntity.anchorVersionLabel}: the review
     * only ever remembers its current head's label, not an older one's. */
    @Column(name = "head_label", nullable = false, length = 64)
    private String headLabel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VerdictEntity() {
        // for JPA
    }

    public VerdictEntity(
            final UUID id,
            final UUID reviewId,
            final String subjectId,
            final ActorKind actorKind,
            final VerdictType verdictType,
            final String body,
            final String headContentSha,
            final String headLabel,
            final Instant now) {
        this.id = id;
        this.reviewId = reviewId;
        this.subjectId = subjectId;
        this.actorKind = actorKind;
        this.verdictType = verdictType;
        this.body = body;
        this.headContentSha = headContentSha;
        this.headLabel = headLabel;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Recast against the same head: the verdict and its body may change, nothing else does. */
    public void recast(final VerdictType verdictType, final String body, final Instant now) {
        this.verdictType = verdictType;
        this.body = body;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public String subjectId() {
        return subjectId;
    }

    public ActorKind actorKind() {
        return actorKind;
    }

    public VerdictType verdictType() {
        return verdictType;
    }

    public String body() {
        return body;
    }

    public String headContentSha() {
        return headContentSha;
    }

    public String headLabel() {
        return headLabel;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
