package com.specforge.review;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A review's head moved to new content, and here is what that did to every section address the old
 * head had. Discussions and agent findings attach to those addresses, so they re-render from this
 * rather than re-deriving it — one classification, applied everywhere.
 *
 * <p>The states are stamped here, at the moment of the carry, and not offered as a query afterwards:
 * SpecForge keeps only a review's current head, so once the head has moved there is no longer any
 * content to compare an older anchor against. Deciding late would mean guessing.
 *
 * @param sections every anchor key the previous head had, mapped to what became of it. A key absent
 *     from this map was never in the previous head, and nothing about it has been decided.
 */
public record ReviewHeadAdvanced(
        UUID reviewId,
        UUID documentId,
        String previousHeadSha,
        String headSha,
        Map<String, AnchorState> sections,
        Instant advancedAt) {}
