package com.specforge.approval;

import java.util.UUID;

/**
 * The automated-check state for a review's current head, declared here and implemented by
 * {@code automated-review}.
 *
 * <p>The port points this way round on purpose: the gate decides what stops an approval, so it
 * states what it needs to know and the capability that runs the checks answers. A check runner
 * therefore cannot widen its own influence over the gate — it can only fill in this answer.
 */
public interface BlockingChecks {

    CheckState state(UUID reviewId);
}
