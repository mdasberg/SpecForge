package com.specforge.repository;

import java.util.UUID;

/**
 * One specification's proposed content, as it stands at the pull request's head.
 *
 * <p>The content travels with the event rather than being left in this module's tables for the
 * review capability to come and read: a review needs the body to diff it, and fetching it across the
 * module boundary would make the review's correctness depend on this module's storage rather than on
 * what it was told.
 *
 * <p>It is the raw file content, not a normalised or hashed form. Normalisation is the catalogue's
 * rule, and a second hash computed under a different rule here would quietly key caches that are
 * supposed to be the same.
 */
public record ProposedSpec(UUID documentId, String path, String content) {}
