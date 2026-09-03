package com.specforge.repository.service;

import com.specforge.platform.api.dto.ImportRun;
import com.specforge.platform.api.dto.ImportRunList;
import com.specforge.repository.entity.ConnectionState;
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
import com.specforge.repository.event.ImportRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts and reads import runs. The work itself happens in {@link ImportRunner}, off the request thread. */
@RequiredArgsConstructor
@Service
@Transactional
public class ImportService {

    private final ImportRunRepository runs;
    private final ImportRunFileRepository runFiles;
    private final ApplicationEventPublisher events;
    private final RepositoryConnectionRepository connections;
    private final Clock clock;

    /** @param onlyPaths the paths to import, or null to import everything the glob matches */
    ImportRunEntity start(final RepositoryConnectionEntity connection, final ImportTrigger trigger,
            final List<String> onlyPaths) {
        if (connection.state() == ConnectionState.DEGRADED) {
            throw Problems.conflict(
                    "The connection to %s is degraded and does not synchronise: %s"
                            .formatted(connection.repositoryFullName(), connection.degradedReason()));
        }
        final ImportRunEntity run = runs.save(
                new ImportRunEntity(UUID.randomUUID(), connection.id(), trigger, clock.instant()));
        events.publishEvent(new ImportRequested(run.id(), onlyPaths));
        return run;
    }

    private RepositoryConnectionEntity require(final UUID connectionId) {
        return connections
                .findById(connectionId)
                .orElseThrow(() -> Problems.notFound("No repository connection %s.".formatted(connectionId)));
    }

    /** The manual re-import the API offers, which is idempotent against unchanged content. */
    public ImportRun startManual(final UUID connectionId) {
        final ImportRunEntity run = start(require(connectionId), ImportTrigger.MANUAL, null);
        return RepositoryMapper.importRun(run, List.of());
    }

    @Transactional(readOnly = true)
    public ImportRunList list(final UUID connectionId) {
        require(connectionId);
        return new ImportRunList(runs.findByConnectionIdOrderByStartedAtDesc(connectionId).stream()
                .map(run -> RepositoryMapper.importRun(run, runFiles.findByRunIdOrderByPathAsc(run.id())))
                .toList());
    }

    @Transactional(readOnly = true)
    public ImportRun get(final UUID connectionId, final UUID runId) {
        final ImportRunEntity run = runs.findByIdAndConnectionId(runId, connectionId)
                .orElseThrow(() -> Problems.notFound("No import run %s on this connection.".formatted(runId)));
        return RepositoryMapper.importRun(run, runFiles.findByRunIdOrderByPathAsc(run.id()));
    }
}
