package com.specforge.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A pull request has proposed new content for one or more specifications. This is the moment a
 * review should exist; creating it belongs to the review capability, so this module only states
 * that it happened and carries the pull request the verdict will have to be reported back to.
 *
 * @param author who pushed the head, which is the attribution a reviewer sees against a changed
 *     section — SpecForge reads the repository, so this is the finest grain it honestly has
 * @param specs the specifications the pull request touches, each with the content proposed for it
 */
public record SpecChangeProposed(
        UUID connectionId,
        UUID proposalId,
        String repositoryFullName,
        int pullRequestNumber,
        String headSha,
        String author,
        List<ProposedSpec> specs,
        Instant proposedAt) {}
