package com.specforge.discussion;

import java.util.UUID;

/** What the rest of SpecForge may read about a review's threads, without reaching into its tables. */
public interface Discussions {

    /**
     * Unresolved threads a human opened. Agents never approve, so a thread an agent opened is
     * never what the approval gate is waiting on.
     */
    int unresolvedBlockingCount(UUID reviewId);
}
