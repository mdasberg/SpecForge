package com.specforge.agent.service;

import com.specforge.agent.event.ChecksQueued;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Executes the queued runs once the transaction that recorded them has committed, off the request
 * thread. Each run gets its own transaction inside {@link CheckExecution}, so a check that fails
 * does not roll back the ones that already finished.
 */
@RequiredArgsConstructor
@Component
class ChecksQueuedListener {

    private final CheckExecution execution;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void on(final ChecksQueued queued) {
        for (final UUID runId : queued.runIds()) {
            execution.execute(runId);
        }
    }
}
