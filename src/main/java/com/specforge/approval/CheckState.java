package com.specforge.approval;

import java.util.List;

/**
 * @param configured whether any check runner is registered for this review at all
 * @param failed whether a blocking check has failed; always false while not configured
 * @param failedCheckNames named for the gate's refusal reason; empty while not failed
 */
public record CheckState(boolean configured, boolean failed, List<String> failedCheckNames) {

    public static final CheckState NOT_CONFIGURED = new CheckState(false, false, List.of());
}
