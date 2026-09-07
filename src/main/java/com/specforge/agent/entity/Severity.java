package com.specforge.agent.entity;

/**
 * How serious the authoring agent considers a finding. It never decides whether the check blocks an
 * approval — the project's {@code blocking} flag on the check definition does — so an agent cannot
 * escalate its own output into a block.
 */
public enum Severity {
    INFO,
    WARNING,
    ERROR
}
