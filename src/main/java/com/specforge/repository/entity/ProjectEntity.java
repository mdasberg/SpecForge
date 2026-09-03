package com.specforge.repository.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The project a connected repository's specifications belong to, as captured in the wizard's third
 * step. The approval rule lives here — minimum approvals and required roles — and is stored
 * verbatim: evaluating it belongs to the approval capability, so this module never reads it.
 */
@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "team", length = 255)
    private String team;

    @Column(name = "tracker", length = 32)
    private String tracker;

    @Column(name = "tracker_project_key", length = 128)
    private String trackerProjectKey;

    @Column(name = "min_approvals", nullable = false)
    private int minApprovals;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_domain", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "domain", nullable = false, length = 255)
    private Set<String> domains;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_required_role", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "role", nullable = false, length = 32)
    private Set<String> requiredRoles;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProjectEntity() {
        // for JPA
    }

    public ProjectEntity(
            final UUID id,
            final String name,
            final String team,
            final String tracker,
            final String trackerProjectKey,
            final int minApprovals,
            final Set<String> domains,
            final Set<String> requiredRoles,
            final Instant now) {
        this.id = id;
        this.name = name;
        this.team = team;
        this.tracker = tracker;
        this.trackerProjectKey = trackerProjectKey;
        this.minApprovals = minApprovals;
        this.domains = new LinkedHashSet<>(domains);
        this.requiredRoles = new LinkedHashSet<>(requiredRoles);
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(
            final String team,
            final String tracker,
            final String trackerProjectKey,
            final int minApprovals,
            final Set<String> domains,
            final Set<String> requiredRoles,
            final Instant now) {
        this.team = team;
        this.tracker = tracker;
        this.trackerProjectKey = trackerProjectKey;
        this.minApprovals = minApprovals;
        this.domains = new LinkedHashSet<>(domains);
        this.requiredRoles = new LinkedHashSet<>(requiredRoles);
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String team() {
        return team;
    }

    public String tracker() {
        return tracker;
    }

    public String trackerProjectKey() {
        return trackerProjectKey;
    }

    public int minApprovals() {
        return minApprovals;
    }

    public Set<String> domains() {
        return Collections.unmodifiableSet(domains);
    }

    public Set<String> requiredRoles() {
        return Collections.unmodifiableSet(requiredRoles);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
