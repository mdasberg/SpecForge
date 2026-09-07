package com.specforge.review;

import java.util.UUID;

/**
 * A review's current head, for a capability that anchors something to it.
 *
 * <p>A pull-request head has no version ordinal, so it cannot be looked up through the catalogue the
 * way an imported version can — this is the only route to its content, and to the label ("v4" or
 * "#12") the rest of the product already shows for it.
 *
 * @param label the same label {@code ReviewMapper} renders for this head
 */
public record ReviewHead(UUID reviewId, UUID documentId, String content, String contentSha, String label) {}
