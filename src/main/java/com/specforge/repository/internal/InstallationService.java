package com.specforge.repository.internal;

import com.specforge.repository.internal.ForgeInstallationEntity.GrantedRepositoryRow;
import com.specforge.repository.internal.forge.Forge;
import com.specforge.repository.internal.forge.ForgeInstallationInfo;
import com.specforge.repository.internal.forge.ForgeRepositoryInfo;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The GitHub App installations SpecForge reads through. An installation arrives over the webhook
 * rather than through a form: installing the App on an account is the act that grants access, and
 * GitHub reports it, so SpecForge never asks a user to type a repository it may not be allowed to
 * read.
 */
@Service
class InstallationService {

    private static final Logger log = LoggerFactory.getLogger(InstallationService.class);

    private final ForgeInstallationRepository installations;
    private final RepositoryConnectionRepository connections;
    private final Forge forge;
    private final Clock clock;

    InstallationService(
            ForgeInstallationRepository installations,
            RepositoryConnectionRepository connections,
            Forge forge,
            Clock clock) {
        this.installations = installations;
        this.connections = connections;
        this.forge = forge;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    List<ForgeInstallationEntity> list() {
        return installations.findAllByOrderByAccountLoginAsc();
    }

    /**
     * Mirrors what the forge says about an installation, and carries the consequence through to
     * every connection made through it. A revoked installation degrades its connections; it never
     * deletes what they imported, because losing access to a repository is not a reason to lose
     * the review history of what it used to hold.
     */
    @Transactional
    void refresh(String externalId) {
        Optional<ForgeInstallationInfo> info = forge.installation(externalId);
        if (info.isEmpty()) {
            installations.findByExternalId(externalId).ifPresent(installation -> {
                installation.changeStatus(InstallationStatus.REVOKED, clock.instant());
                installations.save(installation);
                degradeConnections(installation, "The GitHub App installation was removed.");
            });
            return;
        }
        ForgeInstallationInfo current = info.get();
        Instant now = clock.instant();
        InstallationStatus status = current.suspended() ? InstallationStatus.SUSPENDED : InstallationStatus.ACTIVE;
        Set<GrantedRepositoryRow> repositories = new LinkedHashSet<>();
        for (ForgeRepositoryInfo repository : current.repositories()) {
            repositories.add(new GrantedRepositoryRow(
                    repository.fullName(), repository.externalId(), repository.defaultBranch()));
        }

        ForgeInstallationEntity installation = installations
                .findByExternalId(externalId)
                .orElseGet(() -> installations.save(new ForgeInstallationEntity(
                        UUID.randomUUID(), "GITHUB", externalId, current.accountLogin(), now)));
        installation.refresh(
                current.accountLogin(), current.accountType(), current.accountRepositoryCount(), repositories, now);
        installation.changeStatus(status, now);
        installations.save(installation);

        if (status == InstallationStatus.SUSPENDED) {
            degradeConnections(installation, "The GitHub App installation is suspended.");
        } else {
            restoreConnections(installation, repositories);
        }
    }

    /** Called when the forge reports the installation gone, which the webhook knows before a read fails. */
    @Transactional
    void revoke(String externalId) {
        installations.findByExternalId(externalId).ifPresent(installation -> {
            installation.changeStatus(InstallationStatus.REVOKED, clock.instant());
            installations.save(installation);
            degradeConnections(installation, "The GitHub App installation was revoked.");
        });
    }

    private void degradeConnections(ForgeInstallationEntity installation, String reason) {
        Instant now = clock.instant();
        for (RepositoryConnectionEntity connection : connections.findByInstallationId(installation.id())) {
            if (connection.state() != ConnectionState.DEGRADED) {
                log.info("Degrading connection {} : {}", connection.id(), reason);
                connection.degrade(reason, now);
                connections.save(connection);
            }
        }
    }

    /** A connection only comes back if its repository is still granted; access can return partially. */
    private void restoreConnections(ForgeInstallationEntity installation, Set<GrantedRepositoryRow> repositories) {
        Instant now = clock.instant();
        Set<String> granted = new LinkedHashSet<>();
        for (GrantedRepositoryRow repository : repositories) {
            granted.add(repository.fullName());
        }
        for (RepositoryConnectionEntity connection : connections.findByInstallationId(installation.id())) {
            boolean stillGranted = granted.contains(connection.repositoryFullName());
            if (stillGranted && connection.state() == ConnectionState.DEGRADED) {
                connection.restore(now);
                connections.save(connection);
            } else if (!stillGranted && connection.state() != ConnectionState.DEGRADED) {
                connection.degrade("The installation no longer grants access to this repository.", now);
                connections.save(connection);
            }
        }
    }
}
