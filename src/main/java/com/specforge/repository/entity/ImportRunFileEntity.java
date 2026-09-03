package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** What one import run did to one file, and why, when it did nothing. */
@Entity
@Table(name = "import_run_file")
public class ImportRunFileEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 32)
    private FileOutcome outcome;

    @Column(name = "reason", length = 1024)
    private String reason;

    protected ImportRunFileEntity() {
        // for JPA
    }

    public ImportRunFileEntity(final UUID id, final UUID runId, final String path, final FileOutcome outcome,
            final String reason) {
        this.id = id;
        this.runId = runId;
        this.path = path;
        this.outcome = outcome;
        this.reason = reason == null || reason.length() <= 1024 ? reason : reason.substring(0, 1024);
    }

    public UUID id() {
        return id;
    }

    public UUID runId() {
        return runId;
    }

    public String path() {
        return path;
    }

    public FileOutcome outcome() {
        return outcome;
    }

    public String reason() {
        return reason;
    }
}
