package com.specforge.discussion;

import com.specforge.platform.Caller;
import java.util.UUID;

/** What the rest of SpecForge may read about a review's threads, without reaching into its tables. */
public interface Discussions {

    /**
     * Unresolved threads a human opened. Agents never approve, so a thread an agent opened is
     * never what the approval gate is waiting on.
     */
    int unresolvedBlockingCount(UUID reviewId);

    /**
     * Opens a thread anchored to one section of the review's head, attributed to {@code caller}.
     *
     * <p>It exists for accepting or arguing with an agent's finding: accepting a suggestion opens a
     * thread carrying the proposed text, and the accepting reviewer — not the agent — is its opener,
     * because the reviewer is the one who decided the suggestion was worth acting on. That also
     * makes the resulting thread blocking, which a thread an agent opened would not be.
     *
     * @return the new thread's id
     */
    UUID openOnSection(UUID reviewId, String sectionKey, String body, Caller caller);
}
