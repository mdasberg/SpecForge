package com.specforge.catalog.entity;

import com.specforge.catalog.SpecStatus;
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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A specification document is SpecForge's mirror of one file in a connected repository. Its
 * identity is the connection plus the repository path, so re-importing the same path updates this
 * row rather than creating a second document.
 *
 * <p>{@code connectionId} is a plain id and not a JPA association on purpose: the connection
 * belongs to another Modulith module, and a mapped association there would couple the two
 * modules' persistence.
 */
@Entity
@Table(name = "spec_document")
public class SpecDocument {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "repository_full_name", nullable = false, length = 512)
    private String repositoryFullName;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "project", nullable = false, length = 255)
    private String project;

    @Column(name = "domain", length = 255)
    private String domain;

    @Column(name = "owning_team", length = 255)
    private String owningTeam;

    @Column(name = "owner", length = 255)
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SpecStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "spec_document_tag", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag", nullable = false, length = 128)
    private Set<String> tags;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SpecDocument() {
        // for JPA
    }

    public SpecDocument(
            final UUID id,
            final UUID connectionId,
            final String repositoryFullName,
            final String path,
            final String title,
            final String project,
            final String domain,
            final String owningTeam,
            final String owner,
            final Set<String> tags,
            final Instant now) {
        this.id = id;
        this.connectionId = connectionId;
        this.repositoryFullName = repositoryFullName;
        this.path = path;
        this.title = title;
        this.project = project;
        this.domain = domain;
        this.owningTeam = owningTeam;
        this.owner = owner;
        this.status = SpecStatus.DRAFT;
        this.tags = new LinkedHashSet<>(tags);
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * Applies what the latest import found. Returns whether anything actually changed, so a
     * re-import that finds identical metadata does not write.
     */
    public boolean updateMetadata(final String repositoryFullName, final String title, final String domain,
            final String owningTeam, final String owner, final Set<String> tags, final Instant now) {
        if (this.repositoryFullName.equals(repositoryFullName)
                && this.title.equals(title)
                && Objects.equals(this.domain, domain)
                && Objects.equals(this.owningTeam, owningTeam)
                && Objects.equals(this.owner, owner)
                && this.tags.equals(tags)) {
            return false;
        }
        this.repositoryFullName = repositoryFullName;
        this.title = title;
        this.domain = domain;
        this.owningTeam = owningTeam;
        this.owner = owner;
        this.tags = new LinkedHashSet<>(tags);
        this.updatedAt = now;
        return true;
    }

    /**
     * Assigns the status and {@code updatedAt}. Validates nothing: the module's state machine is
     * the only caller and the only place a transition is judged legal.
     */
    public void changeStatus(final SpecStatus status, final Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID connectionId() {
        return connectionId;
    }

    public String repositoryFullName() {
        return repositoryFullName;
    }

    public String path() {
        return path;
    }

    public String title() {
        return title;
    }

    public String project() {
        return project;
    }

    public String domain() {
        return domain;
    }

    public String owningTeam() {
        return owningTeam;
    }

    public String owner() {
        return owner;
    }

    public SpecStatus status() {
        return status;
    }

    public Set<String> tags() {
        return Collections.unmodifiableSet(tags);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
