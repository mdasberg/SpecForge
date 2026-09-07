package com.specforge.discussion.entity;

import com.specforge.platform.ActorKind;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * One post in a thread — the thread's first comment opens it, every one after that is a reply.
 *
 * <p>The actor kind is stamped here at write time rather than joined from the identity mirror on
 * every read: the API must never be able to omit it, and a value copied once at creation cannot
 * later fail to load.
 */
@Entity
@Table(name = "comment")
public class CommentEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "author", nullable = false, length = 64)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_kind", nullable = false, length = 16)
    private ActorKind actorKind;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mention", joinColumns = @JoinColumn(name = "comment_id"))
    @Column(name = "subject_id", nullable = false, length = 64)
    private Set<String> mentionedSubjectIds = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    protected CommentEntity() {
        // for JPA
    }

    public CommentEntity(
            final UUID id,
            final UUID threadId,
            final String author,
            final ActorKind actorKind,
            final String body,
            final Set<String> mentionedSubjectIds,
            final Instant now) {
        this.id = id;
        this.threadId = threadId;
        this.author = author;
        this.actorKind = actorKind;
        this.body = body;
        this.mentionedSubjectIds = new LinkedHashSet<>(mentionedSubjectIds);
        this.createdAt = now;
    }

    /** Editing is allowed only within a short window of posting; the caller enforces that. */
    public void edit(final String body, final Set<String> mentionedSubjectIds, final Instant now) {
        this.body = body;
        this.mentionedSubjectIds = new LinkedHashSet<>(mentionedSubjectIds);
        this.editedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID threadId() {
        return threadId;
    }

    public String author() {
        return author;
    }

    public ActorKind actorKind() {
        return actorKind;
    }

    public String body() {
        return body;
    }

    public Set<String> mentionedSubjectIds() {
        return Set.copyOf(mentionedSubjectIds);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant editedAt() {
        return editedAt;
    }
}
