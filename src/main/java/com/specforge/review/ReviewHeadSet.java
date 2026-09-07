package com.specforge.review;

import java.time.Instant;
import java.util.UUID;

/**
 * A review now has this head — either because the review just opened, or because a further push
 * moved it. It exists beside {@link ReviewHeadAdvanced} because the two answer different questions:
 * that one says what a move did to the anchors written against the head it replaced, and so cannot
 * be raised for a first head, which had no predecessor and no anchors. Automated checks have to run
 * on a first head as much as on a fifth, so they listen here.
 *
 * @param advanced whether this head replaced an earlier one, which is what makes the previous runs
 *     stale rather than simply absent
 */
public record ReviewHeadSet(
        UUID reviewId,
        UUID documentId,
        String headContentSha,
        String headLabel,
        boolean advanced,
        Instant setAt) {}
