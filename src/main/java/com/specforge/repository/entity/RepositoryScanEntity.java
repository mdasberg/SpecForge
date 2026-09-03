package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A dry run of an import, made before a connection exists so an administrator sees what a path
 * glob would bring in before committing to it. The counts are stored rather than derived: the scan
 * is the evidence the connection is later checked against.
 */
@Entity
@Table(name = "repository_scan")
public class RepositoryScanEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "installation_id", nullable = false)
    private UUID installationId;

    @Column(name = "repository_full_name", nullable = false, length = 512)
    private String repositoryFullName;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "path_glob", nullable = false, length = 512)
    private String pathGlob;

    @Enumerated(EnumType.STRING)
    @Column(name = "spec_format", nullable = false, length = 32)
    private SpecFileFormat specFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ScanStatus status;

    @Column(name = "importable_count", nullable = false)
    private int importableCount;

    @Column(name = "change_proposal_count", nullable = false)
    private int changeProposalCount;

    @Column(name = "unparsable_count", nullable = false)
    private int unparsableCount;

    @Column(name = "failure_reason", length = 1024)
    private String failureReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected RepositoryScanEntity() {
        // for JPA
    }

    public RepositoryScanEntity(
            UUID id,
            UUID installationId,
            String repositoryFullName,
            String branch,
            String pathGlob,
            SpecFileFormat specFormat,
            Instant startedAt) {
        this.id = id;
        this.installationId = installationId;
        this.repositoryFullName = repositoryFullName;
        this.branch = branch;
        this.pathGlob = pathGlob;
        this.specFormat = specFormat;
        this.status = ScanStatus.PENDING;
        this.startedAt = startedAt;
    }

    public void start() {
        this.status = ScanStatus.RUNNING;
    }

    public void succeed(int importableCount, int changeProposalCount, int unparsableCount, Instant finishedAt) {
        this.status = ScanStatus.SUCCEEDED;
        this.importableCount = importableCount;
        this.changeProposalCount = changeProposalCount;
        this.unparsableCount = unparsableCount;
        this.finishedAt = finishedAt;
    }

    public void fail(String failureReason, Instant finishedAt) {
        this.status = ScanStatus.FAILED;
        // Truncated to the column: a stack of nested causes would otherwise fail the insert and
        // lose the reason entirely.
        this.failureReason = failureReason == null || failureReason.length() <= 1024
                ? failureReason
                : failureReason.substring(0, 1024);
        this.finishedAt = finishedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID installationId() {
        return installationId;
    }

    public String repositoryFullName() {
        return repositoryFullName;
    }

    public String branch() {
        return branch;
    }

    public String pathGlob() {
        return pathGlob;
    }

    public SpecFileFormat specFormat() {
        return specFormat;
    }

    public ScanStatus status() {
        return status;
    }

    public int importableCount() {
        return importableCount;
    }

    public int changeProposalCount() {
        return changeProposalCount;
    }

    public int unparsableCount() {
        return unparsableCount;
    }

    public String failureReason() {
        return failureReason;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant finishedAt() {
        return finishedAt;
    }
}
