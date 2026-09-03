package com.specforge.repository.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One execution of an import. It exists so "why is this file not a specification" is answerable
 * afterwards from a row rather than by running the import again and hoping the repository still
 * looks the way it did.
 */
@Entity
@Table(name = "import_run")
public class ImportRunEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_kind", nullable = false, length = 32)
    private ImportTrigger trigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "imported_count", nullable = false)
    private int importedCount;

    @Column(name = "unchanged_count", nullable = false)
    private int unchangedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    protected ImportRunEntity() {
        // for JPA
    }

    ImportRunEntity(UUID id, UUID connectionId, ImportTrigger trigger, Instant startedAt) {
        this.id = id;
        this.connectionId = connectionId;
        this.trigger = trigger;
        this.status = RunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    void resolvedCommit(String commitSha) {
        this.commitSha = commitSha;
    }

    void succeed(int importedCount, int unchangedCount, int skippedCount, int failedCount, Instant finishedAt) {
        this.status = RunStatus.SUCCEEDED;
        this.importedCount = importedCount;
        this.unchangedCount = unchangedCount;
        this.skippedCount = skippedCount;
        this.failedCount = failedCount;
        this.finishedAt = finishedAt;
    }

    void fail(Instant finishedAt) {
        this.status = RunStatus.FAILED;
        this.finishedAt = finishedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID connectionId() {
        return connectionId;
    }

    public ImportTrigger trigger() {
        return trigger;
    }

    public RunStatus status() {
        return status;
    }

    public String commitSha() {
        return commitSha;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }

    public int importedCount() {
        return importedCount;
    }

    public int unchangedCount() {
        return unchangedCount;
    }

    public int skippedCount() {
        return skippedCount;
    }

    public int failedCount() {
        return failedCount;
    }
}
