package com.specforge.review;

import java.util.UUID;

/**
 * A review's identity and lifecycle state, for a capability that has to refuse acting on a closed
 * review and report an outcome back to the pull request that opened it.
 *
 * @param proposalId the proposed change this review follows, or {@code null} for a review opened
 *     directly between two imported versions, which has nowhere to report a status.
 */
public record ReviewRef(UUID reviewId, UUID documentId, boolean open, UUID proposalId) {}
