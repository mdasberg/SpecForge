package com.specforge.repository.service;

import com.specforge.repository.ReviewOutcome;
import com.specforge.repository.ReviewStatusReporter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The published side of {@link ProposalService}: what the approval capability drives later. */
@RequiredArgsConstructor
@Component
public class ReviewStatusReporterAdapter implements ReviewStatusReporter {

    private final ProposalService proposals;

    @Override
    public void report(final UUID proposalId, final ReviewOutcome outcome, final String description) {
        proposals.report(proposalId, outcome, description);
    }
}
