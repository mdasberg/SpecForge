package com.specforge.review;

import java.util.List;
import java.util.UUID;

/**
 * A review's base and head content together with the sections that differ, for a capability that
 * has to read the change rather than render it.
 *
 * <p>The changed section keys come from the review's own cached diff rather than from a second
 * comparison: a model-backed check reads only the changed sections, and if it disagreed with the
 * diff about which those are, a reviewer would be shown a finding on a section their diff says
 * nobody touched.
 *
 * @param changedSectionKeys anchor keys of the added, removed and modified sections, in document
 *     order; empty when the head is identical to the base
 */
public record ReviewComparison(
        UUID reviewId,
        UUID documentId,
        String baseContent,
        String headContent,
        String headContentSha,
        String headLabel,
        List<String> changedSectionKeys) {}
