package com.specforge.repository.entity;

import com.specforge.repository.entity.InstallationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
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
import java.util.Set;
import java.util.UUID;

/**
 * An installation of SpecForge's forge — the grant that lets it read an account, carried through to
 * every connection, scan and import it touches. It is refreshed from the forge's own view of the
 * account, so it stores what the forge reports rather than what the wizard captured.
 *
 * <p>The granted repositories are an element collection on purpose: the wizard says how many the
 * account holds and which are granted in the same snapshot, so they travel with the installation
 * and decide whether a connection is allowed before the connection row is even written.
 */
@Entity
@Table(name = "forge_installation")
public class ForgeInstallationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "external_id", nullable = false, length = 64)
    private String externalId;

    @Column(name = "account_login", nullable = false, length = 255)
    private String accountLogin;

    @Column(name = "account_type", length = 32)
    private String accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InstallationStatus status;

    @Column(name = "account_repository_count")
    private Integer accountRepositoryCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "forge_installation_repository",
            joinColumns = @JoinColumn(name = "installation_id"))
    private Set<GrantedRepositoryRow> repositories;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ForgeInstallationEntity() {
        // for JPA
    }

    public ForgeInstallationEntity(
            final UUID id,
            final String provider,
            final String externalId,
            final String accountLogin,
            final Instant createdAt) {
        this.id = id;
        this.provider = provider;
        this.externalId = externalId;
        this.accountLogin = accountLogin;
        this.status = InstallationStatus.ACTIVE;
        this.repositories = new LinkedHashSet<>();
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Applies the forge's current view of the account. It only rewrites the repositories when the
     * grant actually changed, so a refresh that finds nothing new does not rewrite the collection
     * or a review that watches the row.
     */
    public void refresh(
            final String accountLogin,
            final String accountType,
            final Integer accountRepositoryCount,
            final Set<GrantedRepositoryRow> repositories,
            final Instant now) {
        this.accountLogin = accountLogin;
        this.accountType = accountType;
        this.accountRepositoryCount = accountRepositoryCount;
        this.updatedAt = now;
        if (!this.repositories.equals(repositories)) {
            this.repositories = new LinkedHashSet<>(repositories);
        }
    }

    /**
     * Sets the status and {@code updatedAt}. Nothing is validated here: the installation's own
     * lifecycle is the only caller, and it is the only place a transition is judged legal.
     */
    public void changeStatus(final InstallationStatus status, final Instant now) {
        this.status = status;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public String provider() {
        return provider;
    }

    public String externalId() {
        return externalId;
    }

    public String accountLogin() {
        return accountLogin;
    }

    public String accountType() {
        return accountType;
    }

    public InstallationStatus status() {
        return status;
    }

    public Integer accountRepositoryCount() {
        return accountRepositoryCount;
    }

    public Set<GrantedRepositoryRow> repositories() {
        return Collections.unmodifiableSet(repositories);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    /**
     * One repository an installation grants access to. Stored with the installation rather than a
     * separate row because the wizard asks for the whole grant in one snapshot, and it is what a
     * connection checks against before it is created.
     */
    @Embeddable
    public static class GrantedRepositoryRow {

        @Column(name = "full_name", nullable = false, length = 512)
        private String fullName;

        @Column(name = "external_id", length = 64)
        private String externalId;

        @Column(name = "default_branch", length = 255)
        private String defaultBranch;

        public GrantedRepositoryRow() {}

        public GrantedRepositoryRow(final String fullName, final String externalId, final String defaultBranch) {
            this.fullName = fullName;
            this.externalId = externalId;
            this.defaultBranch = defaultBranch;
        }

        public String fullName() {
            return fullName;
        }

        public String externalId() {
            return externalId;
        }

        public String defaultBranch() {
            return defaultBranch;
        }

        /**
         * Value semantics, because these live in a {@code Set}: without them two rows naming the
         * same repository are different elements, and every refresh looks like a changed grant.
         */
        @Override
        public boolean equals(final Object other) {
            return other instanceof GrantedRepositoryRow row
                    && java.util.Objects.equals(fullName, row.fullName)
                    && java.util.Objects.equals(externalId, row.externalId)
                    && java.util.Objects.equals(defaultBranch, row.defaultBranch);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(fullName, externalId, defaultBranch);
        }
    }
}
