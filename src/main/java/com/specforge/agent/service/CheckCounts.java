package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.RunState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One counting of a review's live runs, used by both readers of them: the approval gate, which cares
 * only about failed blocking checks, and the Checks tab, which shows the lot. Counted once so the
 * panel's summary line and the tab can never disagree about how many checks there are.
 *
 * @param failedBlockingNames the failed blocking checks by display name — the gate's refusal reason
 */
record CheckCounts(
        boolean configured,
        int total,
        int passed,
        int failed,
        int pending,
        List<String> failedBlockingNames) {

    static CheckCounts of(
            final boolean configured,
            final List<CheckRunEntity> liveRuns,
            final Map<UUID, CheckDefinitionEntity> definitions) {
        int passed = 0;
        int failed = 0;
        int pending = 0;
        final List<String> failedBlocking = new ArrayList<>();
        for (final CheckRunEntity run : liveRuns) {
            switch (run.state()) {
                case PASSED -> passed++;
                case FAILED -> {
                    failed++;
                    final CheckDefinitionEntity definition = definitions.get(run.checkDefinitionId());
                    // Blocking is read from the definition now, not from the run: the team's current
                    // decision is the one the gate applies, including to reviews already open.
                    if (definition != null && definition.blocking()) {
                        failedBlocking.add(definition.displayName());
                    }
                }
                case QUEUED, RUNNING -> pending++;
                // A skipped run is neither a pass nor a failure: nobody looked, and the tab says so.
                case SKIPPED -> { }
            }
        }
        return new CheckCounts(configured, liveRuns.size(), passed, failed, pending, List.copyOf(failedBlocking));
    }

    boolean blockingFailure() {
        return !failedBlockingNames.isEmpty();
    }

    static CheckCounts unconfigured() {
        return new CheckCounts(false, 0, 0, 0, 0, List.of());
    }
}
