package com.specforge.agent.entity;

/**
 * Where one run stands. There is no {@code CANCELLED}: a run the head moved past finishes as
 * {@link #SKIPPED} with a summary saying so, because "we did not run this, and here is why" is one
 * fact whether the reason was configuration or a newer head.
 */
public enum RunState {
    QUEUED,
    RUNNING,
    PASSED,
    FAILED,
    SKIPPED;

    public boolean terminal() {
        return this == PASSED || this == FAILED || this == SKIPPED;
    }
}
