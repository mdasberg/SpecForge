package com.specforge.agent.service;

import com.specforge.agent.entity.Severity;

/** One finding a runner produced, before it is stored against the run and its authoring agent. */
record RunnerFinding(
        String sectionKey,
        Integer rangeStart,
        Integer rangeEnd,
        Severity severity,
        String title,
        String body,
        String proposedText) {

    static RunnerFinding on(final String sectionKey, final Severity severity, final String title, final String body) {
        return new RunnerFinding(sectionKey, null, null, severity, title, body, null);
    }
}
