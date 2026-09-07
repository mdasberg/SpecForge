package com.specforge.agent.service;

import com.specforge.agent.entity.RunnerKind;

/**
 * The identities that author findings. They are rows in the same table as every human, holding
 * actor kind {@code AGENT}, which is what makes an unattributed finding unstorable rather than
 * merely discouraged.
 *
 * <p>The ids are pinned in two places — seeded into {@code app_user} by {@code agent-0001-checks},
 * and set on the matching service-account users in {@code keycloak/realm-export.json} — so that a
 * runner writing in process and the same runner presenting a token later are one identity rather
 * than two rows that happen to share a name.
 */
final class AgentIdentities {

    static final String OPENSPEC_VALIDATOR = "a9e15ac6-0000-4000-8000-000000000001";
    static final String SPEC_REVIEWER = "a9e15ac6-0000-4000-8000-000000000002";

    private AgentIdentities() {}

    static String authorOf(final RunnerKind kind) {
        return kind == RunnerKind.MODEL ? SPEC_REVIEWER : OPENSPEC_VALIDATOR;
    }
}
