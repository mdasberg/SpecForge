package com.specforge.review;

import java.util.Optional;
import java.util.UUID;

/** What the rest of SpecForge may read about a review, without reaching into its own tables. */
public interface Reviews {

    /** The review's current head, so a thread anchored against it records the same content a diff would. */
    Optional<ReviewHead> head(UUID reviewId);

    /** The review's identity and lifecycle state, for a capability that gates on it being open. */
    Optional<ReviewRef> ref(UUID reviewId);

    /**
     * Both sides of the review and the sections between them that differ, for a capability that has
     * to check the change itself — an API-compatibility check reads the base, a model-backed one
     * reads only what moved.
     */
    Optional<ReviewComparison> comparison(UUID reviewId);
}
