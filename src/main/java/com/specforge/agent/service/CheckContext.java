package com.specforge.agent.service;

import com.specforge.review.ReviewComparison;
import java.util.List;

/**
 * Everything a runner is given: which check it is running, what that check has configured, and both
 * sides of the review.
 *
 * @param configuration the check definition's configured lines; empty when nothing is configured,
 *     which a check that needs configuration reports as skipped rather than passed
 */
record CheckContext(String checkKey, List<String> configuration, ReviewComparison comparison) {}
