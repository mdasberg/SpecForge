package com.specforge.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One decision about one finding — accepted, dismissed, or that decision undone.
 *
 * <p>The table is append-only and a finding's current disposition is simply its latest row, which
 * is why nothing here is mutable: an undo has to be as visible as the dismissal it reverses, and a
 * row that could be edited would let the record end up saying nobody ever dismissed anything.
 */
@Entity
@Table(name = "finding_disposition")
public class FindingDispositionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "finding_id", nullable = false)
    private UUID findingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private DispositionKind kind;

    @Column(name = "actor_subject_id", nullable = false, length = 64)
    private String actorSubjectId;

    /** The thread an accept opened, carrying the proposed text. Null for a dismiss or an undo. */
    @Column(name = "thread_id")
    private UUID threadId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FindingDispositionEntity() {
        // for JPA
    }

    public FindingDispositionEntity(
            final UUID id,
            final UUID findingId,
            final DispositionKind kind,
            final String actorSubjectId,
            final UUID threadId,
            final Instant now) {
        this.id = id;
        this.findingId = findingId;
        this.kind = kind;
        this.actorSubjectId = actorSubjectId;
        this.threadId = threadId;
        this.createdAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID findingId() {
        return findingId;
    }

    public DispositionKind kind() {
        return kind;
    }

    public String actorSubjectId() {
        return actorSubjectId;
    }

    public UUID threadId() {
        return threadId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
