package com.specforge.repository.internal;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts and reads import runs. The work itself happens in {@link ImportRunner}, off the request thread. */
@Service
class ImportService {

    private final ImportRunRepository runs;
    private final ImportRunFileRepository runFiles;
    private final ImportRunner runner;
    private final Clock clock;

    ImportService(ImportRunRepository runs, ImportRunFileRepository runFiles, ImportRunner runner, Clock clock) {
        this.runs = runs;
        this.runFiles = runFiles;
        this.runner = runner;
        this.clock = clock;
    }

    /**
     * Not transactional: the run row must be committed before the asynchronous runner loads it.
     *
     * @param onlyPaths the paths to import, or null to import everything the glob matches
     */
    ImportRunEntity start(RepositoryConnectionEntity connection, ImportTrigger trigger, List<String> onlyPaths) {
        if (connection.state() == ConnectionState.DEGRADED) {
            throw Problems.conflict(
                    "The connection to %s is degraded and does not synchronise: %s"
                            .formatted(connection.repositoryFullName(), connection.degradedReason()));
        }
        ImportRunEntity run = runs.save(
                new ImportRunEntity(UUID.randomUUID(), connection.id(), trigger, clock.instant()));
        runner.run(run.id(), onlyPaths);
        return run;
    }

    @Transactional(readOnly = true)
    List<ImportRunEntity> list(UUID connectionId) {
        return runs.findByConnectionIdOrderByStartedAtDesc(connectionId);
    }

    @Transactional(readOnly = true)
    ImportRunEntity get(UUID connectionId, UUID runId) {
        return runs.findByIdAndConnectionId(runId, connectionId)
                .orElseThrow(() -> Problems.notFound("No import run %s on this connection.".formatted(runId)));
    }

    @Transactional(readOnly = true)
    List<ImportRunFileEntity> files(UUID runId) {
        return runFiles.findByRunIdOrderByPathAsc(runId);
    }
}
