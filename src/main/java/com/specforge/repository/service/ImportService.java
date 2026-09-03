package com.specforge.repository.service;

import com.specforge.repository.entity.ConnectionState;
import com.specforge.platform.api.dto.ImportRun;
import com.specforge.platform.api.dto.ImportRunList;
import com.specforge.repository.entity.ImportRunEntity;
import com.specforge.repository.entity.ImportRunFileEntity;
import com.specforge.repository.entity.ImportTrigger;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.exception.Problems;
import com.specforge.repository.repository.ImportRunFileRepository;
import com.specforge.repository.repository.ImportRunRepository;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/** Starts and reads import runs. The work itself happens in {@link ImportRunner}, off the request thread. */
@Service
public class ImportService {

    private final ImportRunRepository runs;
    private final ImportRunFileRepository runFiles;
    private final ImportRunner runner;
    private final RepositoryConnectionRepository connections;
    private final Clock clock;

    ImportService(
            ImportRunRepository runs,
            ImportRunFileRepository runFiles,
            ImportRunner runner,
            RepositoryConnectionRepository connections,
            Clock clock) {
        this.runs = runs;
        this.runFiles = runFiles;
        this.runner = runner;
        this.connections = connections;
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

    private RepositoryConnectionEntity require(UUID connectionId) {
        return connections
                .findById(connectionId)
                .orElseThrow(() -> Problems.notFound("No repository connection %s.".formatted(connectionId)));
    }

    /** The manual re-import the API offers, which is idempotent against unchanged content. */
    public ImportRun startManual(UUID connectionId) {
        ImportRunEntity run = start(require(connectionId), ImportTrigger.MANUAL, null);
        return RepositoryMapper.importRun(run, List.of());
    }

    @Transactional(readOnly = true)
    public ImportRunList list(UUID connectionId) {
        require(connectionId);
        return new ImportRunList(runs.findByConnectionIdOrderByStartedAtDesc(connectionId).stream()
                .map(run -> RepositoryMapper.importRun(run, runFiles.findByRunIdOrderByPathAsc(run.id())))
                .toList());
    }

    @Transactional(readOnly = true)
    public ImportRun get(UUID connectionId, UUID runId) {
        ImportRunEntity run = runs.findByIdAndConnectionId(runId, connectionId)
                .orElseThrow(() -> Problems.notFound("No import run %s on this connection.".formatted(runId)));
        return RepositoryMapper.importRun(run, runFiles.findByRunIdOrderByPathAsc(run.id()));
    }
}
