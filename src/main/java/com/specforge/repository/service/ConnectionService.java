package com.specforge.repository.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Connecting a repository, which is the product's front door. Two things are checked before a
 * connection exists at all: that the same repository, branch and path is not already connected,
 * and that the glob actually matches something importable — a connection that imports nothing is a
 * dead end the wizard should not let anyone walk into.
 */
@Service
public class ConnectionService {

    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;
    private final ForgeInstallationRepository installations;
    private final RepositoryScanRepository scans;
    private final ImportService imports;
    private final Forge forge;
    private final Clock clock;

    ConnectionService(
            RepositoryConnectionRepository connections,
            ProjectRepository projects,
            ForgeInstallationRepository installations,
            RepositoryScanRepository scans,
            ImportService imports,
            Forge forge,
            Clock clock) {
        this.connections = connections;
        this.projects = projects;
        this.installations = installations;
        this.scans = scans;
        this.imports = imports;
        this.forge = forge;
        this.clock = clock;
    }

    /**
     * Not transactional as a whole: the connection has to be committed before the initial import
     * can be started against it.
     */
    public RepositoryConnectionEntity create(ConnectionCommand command) {
        ForgeInstallationEntity installation = installations
                .findById(command.installationId())
                .orElseThrow(() -> Problems.notFound("No forge installation %s.".formatted(command.installationId())));
        requireGranted(installation, command.repositoryFullName());
        requireNotAlreadyConnected(command);
        requireSomethingToImport(installation, command);

        Instant now = clock.instant();
        ProjectEntity project = upsertProject(command.project(), now);
        RepositoryConnectionEntity connection = connections.save(new RepositoryConnectionEntity(
                UUID.randomUUID(),
                project.id(),
                installation.id(),
                command.repositoryFullName(),
                command.branch(),
                command.pathGlob(),
                command.specFormat(),
                command.syncMode(),
                now));
        imports.start(connection, ImportTrigger.INITIAL, null);
        return connection;
    }

    @Transactional(readOnly = true)
    public List<RepositoryConnectionEntity> list() {
        return connections.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public RepositoryConnectionEntity get(UUID connectionId) {
        return connections
                .findById(connectionId)
                .orElseThrow(() -> Problems.notFound("No repository connection %s.".formatted(connectionId)));
    }

    @Transactional(readOnly = true)
    public ProjectEntity project(RepositoryConnectionEntity connection) {
        return projects
                .findById(connection.projectId())
                .orElseThrow(() -> Problems.notFound("No project behind connection %s.".formatted(connection.id())));
    }

    private void requireGranted(ForgeInstallationEntity installation, String repositoryFullName) {
        boolean granted = installation.repositories().stream()
                .anyMatch(repository -> repository.fullName().equals(repositoryFullName));
        if (!granted) {
            throw Problems.unprocessable(
                    "The installation on %s does not grant access to %s."
                            .formatted(installation.accountLogin(), repositoryFullName));
        }
    }

    private void requireNotAlreadyConnected(ConnectionCommand command) {
        connections
                .findByRepositoryFullNameAndBranchAndPathGlob(
                        command.repositoryFullName(), command.branch(), command.pathGlob())
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
    private void requireSomethingToImport(ForgeInstallationEntity installation, ConnectionCommand command) {
        if (command.scanId() != null) {
            RepositoryScanEntity scan = scans
                    .findById(command.scanId())
                    .orElseThrow(() -> Problems.notFound("No scan %s.".formatted(command.scanId())));
            if (scan.status() != ScanStatus.SUCCEEDED) {
                throw Problems.unprocessable("Scan %s has not succeeded, so it cannot confirm this connection."
                        .formatted(scan.id()));
            }
            if (scan.importableCount() == 0) {
                throw noSpecifications(command);
            }
            return;
        }
        List<String> matched = SpecPaths.matching(
                forge.listFiles(installation.externalId(), new ForgeRef(command.repositoryFullName(), command.branch())),
                command.pathGlob());
        if (matched.isEmpty()) {
            throw noSpecifications(command);
        }
    }

    private static org.springframework.web.ErrorResponseException noSpecifications(ConnectionCommand command) {
        return Problems.unprocessable("No specification was found at %s on %s (%s)."
                .formatted(command.pathGlob(), command.repositoryFullName(), command.branch()));
    }

    private ProjectEntity upsertProject(ProjectCommand command, Instant now) {
        Set<String> domains = new LinkedHashSet<>(command.domains() == null ? List.of() : command.domains());
        Set<String> requiredRoles = new LinkedHashSet<>(command.requiredRoles() == null ? List.of() : command.requiredRoles());
        return projects
                .findByName(command.name())
                .map(project -> {
                    project.update(
                            command.team(),
                            command.tracker(),
                            command.trackerProjectKey(),
                            command.minApprovals(),
                            domains,
                            requiredRoles,
                            now);
                    return projects.save(project);
                })
                .orElseGet(() -> projects.save(new ProjectEntity(
                        UUID.randomUUID(),
                        command.name(),
                        command.team(),
                        command.tracker(),
                        command.trackerProjectKey(),
                        command.minApprovals(),
                        domains,
                        requiredRoles,
                        now)));
    }
}
