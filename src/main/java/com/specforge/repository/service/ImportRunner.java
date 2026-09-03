package com.specforge.repository.service;

import com.specforge.repository.entity.ImportRunEntity;
import com.specforge.repository.entity.ProjectEntity;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.exception.ForgeException;
import com.specforge.repository.forge.Forge;
import com.specforge.repository.forge.ForgeRef;
import com.specforge.repository.repository.ForgeInstallationRepository;
import com.specforge.repository.repository.ImportRunRepository;
import com.specforge.repository.repository.ProjectRepository;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.specforge.repository.event.ImportRequested;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Executes one import run. It resolves the branch to a commit first and imports at that commit, so
 * every file in a run comes from the same repository state even if someone pushes while it runs.
 */
@RequiredArgsConstructor
@Component
public class ImportRunner {

    private static final Logger log = LoggerFactory.getLogger(ImportRunner.class);

    private final ImportRunRepository runs;
    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;
    private final ForgeInstallationRepository installations;
    private final SpecImporter importer;
    private final Forge forge;
    private final Clock clock;

    /** Runs after the transaction that recorded the run has committed; see {@link ScanRunner}. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void run(final ImportRequested requested) {
        final UUID runId = requested.runId();
        final List<String> onlyPaths = requested.onlyPaths();
        final ImportRunEntity run = runs.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Import run {} vanished before it ran", runId);
            return;
        }
        try {
            final RepositoryConnectionEntity connection = connections
                    .findById(run.connectionId())
                    .orElseThrow(() -> new ForgeException("The connection behind this run no longer exists."));
            final ProjectEntity project = projects
                    .findById(connection.projectId())
                    .orElseThrow(() -> new ForgeException("The project behind this connection no longer exists."));
            final String installationExternalId = installations
                    .findById(connection.installationId())
                    .orElseThrow(() -> new ForgeException("The installation behind this connection no longer exists."))
                    .externalId();

            final String commitSha = forge.headCommit(installationExternalId,
                    new ForgeRef(connection.repositoryFullName(), connection.branch()))
                    .orElseThrow(() -> new ForgeException(
                            "Branch %s does not exist on %s.".formatted(connection.branch(),
                                    connection.repositoryFullName())));
            run.resolvedCommit(commitSha);
            runs.save(run);

            final ForgeRef ref = new ForgeRef(connection.repositoryFullName(), commitSha);
            final List<String> paths = onlyPaths != null
                    ? SpecPaths.matching(onlyPaths, connection.pathGlob())
                    : SpecPaths.matching(forge.listFiles(installationExternalId, ref), connection.pathGlob());

            final ImportSummary summary = importer.importPaths(connection, project, installationExternalId, ref,
                    paths, run.id());
            run.succeed(summary.imported(), summary.unchanged(), summary.skipped(), summary.failed(), clock.instant());
            runs.save(run);
        } catch (final RuntimeException e) {
            log.warn("Import run {} failed", runId, e);
            run.fail(clock.instant());
            runs.save(run);
        }
    }
}
