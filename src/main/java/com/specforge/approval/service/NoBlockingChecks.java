package com.specforge.approval.service;

import com.specforge.approval.BlockingChecks;
import com.specforge.approval.CheckState;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Stands in until {@code add-automated-review} ships a real {@link BlockingChecks}. */
@Component
class NoBlockingChecks implements BlockingChecks {

    @Override
    public CheckState state(final UUID reviewId) {
        return CheckState.NOT_CONFIGURED;
    }
}
