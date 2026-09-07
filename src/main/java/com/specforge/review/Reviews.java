package com.specforge.review;

import java.util.Optional;
import java.util.UUID;

/** What the rest of SpecForge may read about a review, without reaching into its own tables. */
public interface Reviews {

    /** The review's current head, so a thread anchored against it records the same content a diff would. */
    Optional<ReviewHead> head(UUID reviewId);
}
