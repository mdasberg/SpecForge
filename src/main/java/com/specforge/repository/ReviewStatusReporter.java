package com.specforge.repository;

import java.util.UUID;

/**
 * Reports a review's state back onto the pull request that proposed the change, as a commit
 * status. The approval capability drives this once verdicts exist; this module posts the pending
 * status itself when the change is first proposed.
 *
 * <p>It is the only outbound write SpecForge performs on a connected repository, and it writes a
 * signal rather than content.
 */
public interface ReviewStatusReporter {

    void report(UUID proposalId, ReviewOutcome outcome, String description);
}
