package com.specforge.repository.service;

import com.specforge.platform.api.dto.Connection;
import com.specforge.platform.api.dto.ConnectionList;
import com.specforge.platform.api.dto.ConnectionRequest;
import com.specforge.platform.api.dto.ProjectConfiguration;
import com.specforge.repository.entity.ForgeInstallationEntity;
import com.specforge.repository.entity.ImportTrigger;
import com.specforge.repository.entity.ProjectEntity;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.entity.RepositoryScanEntity;
import com.specforge.repository.entity.ScanStatus;
import com.specforge.repository.exception.Problems;
import com.specforge.repository.forge.Forge;
import com.specforge.repository.forge.ForgeRef;
import com.specforge.repository.repository.ForgeInstallationRepository;
import com.specforge.repository.repository.ProjectRepository;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import com.specforge.repository.repository.RepositoryScanRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Connecting a repository, which is the product's front door. Two things are checked before a
 * connection exists at all: that the same repository, branch and path is not already connected,
 * and that the glob actually matches something importable — a connection that imports nothing is a
 * dead end the wizard should not let anyone walk into.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class ConnectionService {

    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;
    private final ForgeInstallationRepository installations;
    private final RepositoryScanRepository scans;
    private final ImportService imports;
    private final Forge forge;
    private final Clock clock;

    public Connection create(final ConnectionRequest request) {
        final ForgeInstallationEntity installation = installations
                .findById(request.getInstallationId())
                .orElseThrow(() -> Problems.notFound("No forge installation %s.".formatted(request.getInstallationId())));
        requireGranted(installation, request.getRepositoryFullName());
        requireNotAlreadyConnected(request);
        requireSomethingToImport(installation, request);

        final Instant now = clock.instant();
        final ProjectEntity project = upsertProject(request.getProject(), now);
        final RepositoryConnectionEntity connection = connections.save(new RepositoryConnectionEntity(
                UUID.randomUUID(),
                project.id(),
                installation.id(),
                request.getRepositoryFullName(),
                request.getBranch(),
                request.getPathGlob(),
                RepositoryMapper.format(request.getSpecFormat()),
                RepositoryMapper.syncPolicy(request.getSyncMode()),
                now));
        imports.start(connection, ImportTrigger.INITIAL, null);
        return RepositoryMapper.connection(connection, project.name());
    }

    @Transactional(readOnly = true)
    public ConnectionList list() {
        return new ConnectionList(connections.findAllByOrderByCreatedAtAsc().stream()
                .map(connection -> RepositoryMapper.connection(connection, project(connection).name()))
                .toList());
    }

    @Transactional(readOnly = true)
    public Connection get(final UUID connectionId) {
        final RepositoryConnectionEntity connection = require(connectionId);
        return RepositoryMapper.connection(connection, project(connection).name());
    }

    /** The row itself, for the services in this package that work on it rather than render it. */
    @Transactional(readOnly = true)
    RepositoryConnectionEntity require(final UUID connectionId) {
        return connections
                .findById(connectionId)
                .orElseThrow(() -> Problems.notFound("No repository connection %s.".formatted(connectionId)));
    }

    ProjectEntity project(final RepositoryConnectionEntity connection) {
        return projects
                .findById(connection.projectId())
                .orElseThrow(() -> Problems.notFound("No project behind connection %s.".formatted(connection.id())));
    }

    private void requireGranted(final ForgeInstallationEntity installation, final String repositoryFullName) {
        final boolean granted = installation.repositories().stream()
                .anyMatch(repository -> repository.fullName().equals(repositoryFullName));
        if (!granted) {
            throw Problems.unprocessable(
                    "The installation on %s does not grant access to %s."
                            .formatted(installation.accountLogin(), repositoryFullName));
        }
    }

    private void requireNotAlreadyConnected(final ConnectionRequest request) {
        connections
                .findByRepositoryFullNameAndBranchAndPathGlob(
                        request.getRepositoryFullName(), request.getBranch(), request.getPathGlob())
                .ifPresent(existing -> {
                    throw Problems.conflict(
                            "%s (%s, %s) is already connected as connection %s."
                                    .formatted(
                                            existing.repositoryFullName(),
                                            existing.branch(),
                                            existing.pathGlob(),
                                            existing.id()));
                });
    }

    /**
     * The scan the administrator confirmed is the evidence when it is given; without one the glob
     * is checked live, so the wizard cannot be bypassed by posting straight at the API.
     */
    private void requireSomethingToImport(final ForgeInstallationEntity installation, final ConnectionRequest request) {
        if (request.getScanId() != null) {
            final RepositoryScanEntity scan = scans
                    .findById(request.getScanId())
                    .orElseThrow(() -> Problems.notFound("No scan %s.".formatted(request.getScanId())));
            if (scan.status() != ScanStatus.SUCCEEDED) {
                throw Problems.unprocessable("Scan %s has not succeeded, so it cannot confirm this connection."
                        .formatted(scan.id()));
            }
            if (scan.importableCount() == 0) {
                throw noSpecifications(request);
            }
            return;
        }
        final List<String> matched = SpecPaths.matching(
                forge.listFiles(
                        installation.externalId(),
                        new ForgeRef(request.getRepositoryFullName(), request.getBranch())),
                request.getPathGlob());
        if (matched.isEmpty()) {
            throw noSpecifications(request);
        }
    }

    private static org.springframework.web.ErrorResponseException noSpecifications(final ConnectionRequest request) {
        return Problems.unprocessable("No specification was found at %s on %s (%s)."
                .formatted(request.getPathGlob(), request.getRepositoryFullName(), request.getBranch()));
    }

    private ProjectEntity upsertProject(final ProjectConfiguration configuration, final Instant now) {
        final Set<String> domains = new LinkedHashSet<>(
                configuration.getDomains() == null ? List.of() : configuration.getDomains());
        final Set<String> requiredRoles = new LinkedHashSet<>(configuration.getApprovalRule().getRequiredRoles());
        final String tracker = configuration.getTracker() == null ? null : configuration.getTracker().name();
        final int minApprovals = configuration.getApprovalRule().getMinApprovals();
        return projects
                .findByName(configuration.getName())
                .map(project -> {
                    project.update(
                            configuration.getTeam(),
                            tracker,
                            configuration.getTrackerProjectKey(),
                            minApprovals,
                            domains,
                            requiredRoles,
                            now);
                    return projects.save(project);
                })
                .orElseGet(() -> projects.save(new ProjectEntity(
                        UUID.randomUUID(),
                        configuration.getName(),
                        configuration.getTeam(),
                        tracker,
                        configuration.getTrackerProjectKey(),
                        minApprovals,
                        domains,
                        requiredRoles,
                        now)));
    }
}
