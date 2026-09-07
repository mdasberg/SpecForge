package com.specforge.approval;

import java.util.List;

/**
 * What the automated checks say about a review, as the gate and the approval panel need it.
 *
 * <p>Only the blocking checks are the gate's business, which is why {@code failed} and
 * {@code failedCheckNames} cover those alone while the counts cover every run: an advisory failure
 * has to be visible without ever being a reason a review cannot be approved.
 *
 * @param configured whether any check is enabled for this review's project at all
 * @param failed whether a blocking check has failed; always false while not configured
 * @param failedCheckNames named for the gate's refusal reason; empty while not failed
 * @param totalCount runs against the review's current head
 * @param passedCount of those, the ones that passed
 * @param failedCount of those, the ones that failed, advisory ones included
 * @param pendingCount of those, the ones still queued or running
 */
public record CheckState(
        boolean configured,
        boolean failed,
        List<String> failedCheckNames,
        int totalCount,
        int passedCount,
        int failedCount,
        int pendingCount) {

    public static final CheckState NOT_CONFIGURED = new CheckState(false, false, List.of(), 0, 0, 0, 0);
}
