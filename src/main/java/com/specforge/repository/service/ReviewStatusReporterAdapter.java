package com.specforge.repository.service;

import com.specforge.repository.ReviewOutcome;
import com.specforge.repository.ReviewStatusReporter;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** The published side of {@link ProposalService}: what the approval capability drives later. */
@Component
public class ReviewStatusReporterAdapter implements ReviewStatusReporter {

    private final ProposalService proposals;

    ReviewStatusReporterAdapter(ProposalService proposals) {
        this.proposals = proposals;
    }

    @Override
    public void report(UUID proposalId, ReviewOutcome outcome, String description) {
        proposals.report(proposalId, outcome, description);
    }
}
