package com.specforge.discussion.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One in-app notification: a mention, or a reply on a thread the recipient participates in. */
@Entity
@Table(name = "notification")
public class NotificationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "recipient_subject_id", nullable = false, length = 64)
    private String recipientSubjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 24)
    private NotificationType type;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @Column(name = "actor_subject_id", nullable = false, length = 64)
    private String actorSubjectId;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected NotificationEntity() {
        // for JPA
    }

    public NotificationEntity(
            final UUID id,
            final String recipientSubjectId,
            final NotificationType type,
            final UUID reviewId,
            final UUID threadId,
            final UUID commentId,
            final String actorSubjectId,
            final Instant now) {
        this.id = id;
        this.recipientSubjectId = recipientSubjectId;
        this.type = type;
        this.reviewId = reviewId;
        this.threadId = threadId;
        this.commentId = commentId;
        this.actorSubjectId = actorSubjectId;
        this.createdAt = now;
    }

    public void markRead(final Instant now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }

    public UUID id() {
        return id;
    }

    public String recipientSubjectId() {
        return recipientSubjectId;
    }

    public NotificationType type() {
        return type;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public UUID threadId() {
        return threadId;
    }

    public UUID commentId() {
        return commentId;
    }

    public String actorSubjectId() {
        return actorSubjectId;
    }

    public Instant readAt() {
        return readAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
