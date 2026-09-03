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
 * A project's link to the repository holding its specifications. Losing access degrades the
 * connection rather than deleting it: what was imported stays readable, because losing a grant is
 * not a reason to lose the review history of what the repository used to hold.
 */
@Entity
@Table(name = "repository_connection")
public class RepositoryConnectionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

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
    @Column(name = "sync_mode", nullable = false, length = 32)
    private SyncPolicy syncMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private ConnectionState state;

    @Column(name = "degraded_reason", length = 512)
    private String degradedReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepositoryConnectionEntity() {
        // for JPA
    }

    public RepositoryConnectionEntity(
            UUID id,
            UUID projectId,
            UUID installationId,
            String repositoryFullName,
            String branch,
            String pathGlob,
            SpecFileFormat specFormat,
            SyncPolicy syncMode,
            Instant now) {
        this.id = id;
        this.projectId = projectId;
        this.installationId = installationId;
        this.repositoryFullName = repositoryFullName;
        this.branch = branch;
        this.pathGlob = pathGlob;
        this.specFormat = specFormat;
        this.syncMode = syncMode;
        this.state = ConnectionState.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void degrade(String reason, Instant now) {
        this.state = ConnectionState.DEGRADED;
        this.degradedReason = reason;
        this.updatedAt = now;
    }

    public void restore(Instant now) {
        this.state = ConnectionState.ACTIVE;
        this.degradedReason = null;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return projectId;
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

    public SyncPolicy syncMode() {
        return syncMode;
    }

    public ConnectionState state() {
        return state;
    }

    public String degradedReason() {
        return degradedReason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
