package com.specforge.repository.internal;

import com.specforge.repository.internal.forge.Forge;
import com.specforge.repository.internal.forge.ForgeException;
import com.specforge.repository.internal.forge.ForgeRef;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Executes one import run. It resolves the branch to a commit first and imports at that commit, so
 * every file in a run comes from the same repository state even if someone pushes while it runs.
 */
@Component
class ImportRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);

    private final ImportRunRepository runs;
    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;
    private final ForgeInstallationRepository installations;
    private final SpecImporter importer;
    private final Forge forge;
    private final Clock clock;

    ImportRunner(
            ImportRunRepository runs,
            RepositoryConnectionRepository connections,
            ProjectRepository projects,
            ForgeInstallationRepository installations,
            SpecImporter importer,
            Forge forge,
            Clock clock) {
        this.runs = runs;
        this.connections = connections;
        this.projects = projects;
        this.installations = installations;
        this.importer = importer;
        this.forge = forge;
        this.clock = clock;
    }

    @Async
    void run(UUID runId, List<String> onlyPaths) {
        ImportRunEntity run = runs.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Import run {} vanished before it ran", runId);
            return;
        }
        try {
            RepositoryConnectionEntity connection = connections
                    .findById(run.connectionId())
                    .orElseThrow(() -> new ForgeException("The connection behind this run no longer exists."));
            ProjectEntity project = projects
                    .findById(connection.projectId())
                    .orElseThrow(() -> new ForgeException("The project behind this connection no longer exists."));
            String installationExternalId = installations
                    .findById(connection.installationId())
                    .orElseThrow(() -> new ForgeException("The installation behind this connection no longer exists."))
                    .externalId();

            String commitSha = forge.headCommit(installationExternalId, new ForgeRef(connection.repositoryFullName(), connection.branch()))
                    .orElseThrow(() -> new ForgeException(
                            "Branch %s does not exist on %s.".formatted(connection.branch(), connection.repositoryFullName())));
            run.resolvedCommit(commitSha);
            runs.save(run);

            ForgeRef ref = new ForgeRef(connection.repositoryFullName(), commitSha);
            List<String> paths = onlyPaths != null
                    ? SpecPaths.matching(onlyPaths, connection.pathGlob())
                    : SpecPaths.matching(forge.listFiles(installationExternalId, ref), connection.pathGlob());

            ImportSummary summary = importer.importPaths(connection, project, installationExternalId, ref, paths, run.id());
            run.succeed(summary.imported(), summary.unchanged(), summary.skipped(), summary.failed(), clock.instant());
            runs.save(run);
        } catch (RuntimeException e) {
            log.warn("Import run {} failed", runId, e);
            run.fail(clock.instant());
            runs.save(run);
        }
    }
}
