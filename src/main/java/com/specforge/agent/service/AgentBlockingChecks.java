package com.specforge.agent.service;

import com.specforge.approval.BlockingChecks;
import com.specforge.approval.CheckState;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * What the gate is told about this review's checks.
 *
 * <p>Only a failed blocking check is reported as blocking. An advisory failure travels in the counts
 * so it stays visible, and a queued or running check blocks nothing either: the requirement is that
 * no blocking check has <em>failed</em>, and treating "not finished yet" as a failure would make the
 * gate a race.
 */
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class AgentBlockingChecks implements BlockingChecks {

    private final CheckService checks;

    @Override
    public CheckState state(final UUID reviewId) {
        final CheckCounts counts = checks.counts(reviewId);
        if (!counts.configured()) {
            return CheckState.NOT_CONFIGURED;
        }
        return new CheckState(
                true,
                counts.blockingFailure(),
                counts.failedBlockingNames(),
                counts.total(),
                counts.passed(),
                counts.failed(),
                counts.pending());
    }
}
