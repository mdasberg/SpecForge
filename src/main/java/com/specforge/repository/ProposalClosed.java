package com.specforge.repository;

import java.time.Instant;
import java.util.UUID;

/**
 * The pull request behind a proposed change was merged or closed, so nothing further will be pushed
 * to it. Whether that ends a review is the review capability's judgement, not this module's.
 */
public record ProposalClosed(UUID proposalId, boolean merged, Instant closedAt) {}
