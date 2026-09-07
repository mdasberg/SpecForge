package com.specforge.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One check a project has enabled, and whether failing it stops an approval.
 *
 * <p>{@code blocking} lives here, on the project's definition, and nowhere near the agent that runs
 * the check: the team decides what may stop a review, and a runner that could mark its own result
 * blocking would be deciding that for them.
 */
@Entity
@Table(name = "check_definition")
public class CheckDefinitionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "check_key", nullable = false, length = 64)
    private String checkKey;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "blocking", nullable = false)
    private boolean blocking;

    @Enumerated(EnumType.STRING)
    @Column(name = "runner_kind", nullable = false, length = 16)
    private RunnerKind runnerKind;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** One entry per line: the architecture rules to test, or the glossary's preferred terms. */
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CheckDefinitionEntity() {
        // for JPA
    }

    public CheckDefinitionEntity(
            final UUID id,
            final UUID projectId,
            final String checkKey,
            final String displayName,
            final boolean blocking,
            final RunnerKind runnerKind,
            final boolean enabled,
            final Instant now) {
        this.id = id;
        this.projectId = projectId;
        this.checkKey = checkKey;
        this.displayName = displayName;
        this.blocking = blocking;
        this.runnerKind = runnerKind;
        this.enabled = enabled;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID projectId() {
        return projectId;
    }

    public String checkKey() {
        return checkKey;
    }

    public String displayName() {
        return displayName;
    }

    public boolean blocking() {
        return blocking;
    }

    public RunnerKind runnerKind() {
        return runnerKind;
    }

    public boolean enabled() {
        return enabled;
    }

    /** The configured lines, blank ones dropped; empty when this check has nothing configured. */
    public List<String> configurationLines() {
        if (configuration == null || configuration.isBlank()) {
            return List.of();
        }
        return configuration.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
