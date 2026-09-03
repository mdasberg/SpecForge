package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Roles come from Keycloak and from nowhere else, which is only true if taking one away in Keycloak
 * actually takes it away here. The mirror is a cache of the token, so a stale role would be a
 * privilege that outlives the decision to revoke it.
 */
class RoleRemovalTest extends BaseIntegrationTest {

    @Test
    void dropsARoleRemovedInKeycloakOnTheNextToken() throws Exception {
        String admin = Keycloak.adminToken();
        String userId = Keycloak.userId(admin, "architect");
        String architectRole = Keycloak.realmRole(admin, "ARCHITECT");

        assertThat(get("/api/me", Keycloak.passwordToken("architect", "architect")).body())
                .as("the role is present before it is revoked")
                .contains("\"ARCHITECT\"");

        Keycloak.removeRealmRole(admin, userId, architectRole);
        try {
            String body = get("/api/me", Keycloak.passwordToken("architect", "architect")).body();

            assertThat(body).doesNotContain("\"ARCHITECT\"");
            assertThat(body).as("still an identity, just a lesser one").contains("\"REVIEWER\"");
        } finally {
            // The realm is shared with every other test in this suite, so put it back even if the
            // assertions fail.
            Keycloak.addRealmRole(admin, userId, architectRole);
        }

        assertThat(get("/api/me", Keycloak.passwordToken("architect", "architect")).body())
                .as("restored for the tests that follow")
                .contains("\"ARCHITECT\"");
    }
}
