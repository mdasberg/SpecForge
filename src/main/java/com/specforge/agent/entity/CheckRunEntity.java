package com.specforge.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One execution of one check against one review head.
 *
 * <p>A run is never rewritten by the next one and never deleted: it is marked stale, which is what
 * lets a reviewer see what the previous head said and lets "this finding is new since the last push"
 * be a comparison rather than a guess. The head sha it was dispatched against is stored for the same
 * reason the verdict stores one — a result about content is worthless without naming the content.
 */
@Entity
@Table(name = "check_run")
public class CheckRunEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    // Plain UUIDs rather than associations: review owns its row, and the definition is read
    // through its own repository so that a run never carries a second, staler copy of it.
    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "check_definition_id", nullable = false)
    private UUID checkDefinitionId;

    @Column(name = "head_content_sha", nullable = false, length = 64)
    private String headContentSha;

    @Column(name = "head_label", nullable = false, length = 64)
    private String headLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private RunState state;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    /** Which model produced this run's findings, for a model-backed check; null for a parser. */
    @Column(name = "model_identity", length = 128)
    private String modelIdentity;

    @Column(name = "stale", nullable = false)
    private boolean stale;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckRunEntity() {
        // for JPA
    }

    public CheckRunEntity(
            final UUID id,
            final UUID reviewId,
            final UUID checkDefinitionId,
            final String headContentSha,
            final String headLabel,
            final Instant now) {
        this.id = id;
        this.reviewId = reviewId;
        this.checkDefinitionId = checkDefinitionId;
        this.headContentSha = headContentSha;
        this.headLabel = headLabel;
        this.state = RunState.QUEUED;
        this.stale = false;
        this.queuedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void start(final Instant now) {
        this.state = RunState.RUNNING;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void finish(final RunState outcome, final String summary, final String modelIdentity, final Instant now) {
        this.state = outcome;
        this.summary = summary;
        this.modelIdentity = modelIdentity;
        this.finishedAt = now;
        this.durationMs = startedAt == null ? null : Duration.between(startedAt, now).toMillis();
        this.updatedAt = now;
    }

    /** Superseded, either by a rerun or by a new head. The result stays readable. */
    public void markStale(final Instant now) {
        this.stale = true;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public UUID checkDefinitionId() {
        return checkDefinitionId;
    }

    public String headContentSha() {
        return headContentSha;
    }

    public String headLabel() {
        return headLabel;
    }

    public RunState state() {
        return state;
    }

    public String summary() {
        return summary;
    }

    public String modelIdentity() {
        return modelIdentity;
    }

    public boolean stale() {
        return stale;
    }

    public Long durationMs() {
        return durationMs;
    }

    public Instant queuedAt() {
        return queuedAt;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
