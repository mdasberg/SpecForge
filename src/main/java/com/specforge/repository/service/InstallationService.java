package com.specforge.repository.service;

import com.specforge.platform.api.dto.ForgeInstallationList;
import com.specforge.repository.entity.ConnectionState;
import com.specforge.repository.entity.ForgeInstallationEntity.GrantedRepositoryRow;
import com.specforge.repository.entity.ForgeInstallationEntity;
import com.specforge.repository.entity.InstallationStatus;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.forge.Forge;
import com.specforge.repository.forge.ForgeInstallationInfo;
import com.specforge.repository.forge.ForgeRepositoryInfo;
import com.specforge.repository.repository.ForgeInstallationRepository;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Service
@Transactional
public class InstallationService {

    private static final Logger log = LoggerFactory.getLogger(InstallationService.class);

    private final ForgeInstallationRepository installations;
    private final RepositoryConnectionRepository connections;
    private final Forge forge;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ForgeInstallationList list() {
        return new ForgeInstallationList(installations.findAllByOrderByAccountLoginAsc().stream()
                .map(RepositoryMapper::installation)
                .toList());
    }

    /**
     * Mirrors what the forge says about an installation, and carries the consequence through to
     * every connection made through it. A revoked installation degrades its connections; it never
     * deletes what they imported, because losing access to a repository is not a reason to lose
     * the review history of what it used to hold.
     */
    @Transactional
    public void refresh(final String externalId) {
        final Optional<ForgeInstallationInfo> info = forge.installation(externalId);
        if (info.isEmpty()) {
            installations.findByExternalId(externalId).ifPresent(installation -> {
                installation.changeStatus(InstallationStatus.REVOKED, clock.instant());
                installations.save(installation);
                degradeConnections(installation, "The GitHub App installation was removed.");
            });
            return;
        }
        final ForgeInstallationInfo current = info.get();
        final Instant now = clock.instant();
        final InstallationStatus status = current.suspended() ? InstallationStatus.SUSPENDED : InstallationStatus.ACTIVE;
        final Set<GrantedRepositoryRow> repositories = new LinkedHashSet<>();
        for (final ForgeRepositoryInfo repository : current.repositories()) {
            repositories.add(new GrantedRepositoryRow(
                    repository.fullName(), repository.externalId(), repository.defaultBranch()));
        }

        final ForgeInstallationEntity installation = installations
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
    public void revoke(final String externalId) {
        installations.findByExternalId(externalId).ifPresent(installation -> {
            installation.changeStatus(InstallationStatus.REVOKED, clock.instant());
            installations.save(installation);
            degradeConnections(installation, "The GitHub App installation was revoked.");
        });
    }

    private void degradeConnections(final ForgeInstallationEntity installation, final String reason) {
        final Instant now = clock.instant();
        for (final RepositoryConnectionEntity connection : connections.findByInstallationId(installation.id())) {
            if (connection.state() != ConnectionState.DEGRADED) {
                log.info("Degrading connection {} : {}", connection.id(), reason);
                connection.degrade(reason, now);
                connections.save(connection);
            }
        }
    }

    /** A connection only comes back if its repository is still granted; access can return partially. */
    private void restoreConnections(final ForgeInstallationEntity installation,
            final Set<GrantedRepositoryRow> repositories) {
        final Instant now = clock.instant();
        final Set<String> granted = new LinkedHashSet<>();
        for (final GrantedRepositoryRow repository : repositories) {
            granted.add(repository.fullName());
        }
        for (final RepositoryConnectionEntity connection : connections.findByInstallationId(installation.id())) {
            final boolean stillGranted = granted.contains(connection.repositoryFullName());
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
