package com.specforge.agent.service;

import com.specforge.agent.entity.RunState;
import java.util.List;

/**
 * What a run concluded. A skipped outcome is deliberately distinct from a passed one: a check that
 * had nothing configured, or no model to ask, has found nothing rather than confirmed nothing is
 * wrong, and a reviewer reading the tab is entitled to that difference.
 *
 * @param modelIdentity the model that produced these findings, or null for a parser
 */
record CheckOutcome(RunState state, String summary, String modelIdentity, List<RunnerFinding> findings) {

    static CheckOutcome passed(final String summary) {
        return new CheckOutcome(RunState.PASSED, summary, null, List.of());
    }

    static CheckOutcome failed(final String summary, final List<RunnerFinding> findings) {
        return new CheckOutcome(RunState.FAILED, summary, null, findings);
    }

    static CheckOutcome skipped(final String summary) {
        return new CheckOutcome(RunState.SKIPPED, summary, null, List.of());
    }

    CheckOutcome from(final String identity) {
        return new CheckOutcome(state, summary, identity, findings);
    }
}
