package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * The identity is mirrored from the token, and the roles come from the realm rather than from
 * anything SpecForge stores.
 */
class IdentityMirrorTest extends BaseIntegrationTest {

    @Test
    void mirrorsAHumanWithTheRealmRolesTheTokenCarries() throws Exception {
        HttpResponse<String> response = get("/api/me", Keycloak.passwordToken("architect", "architect"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body())
                .contains("\"actorKind\":\"HUMAN\"")
                .contains("\"ARCHITECT\"")
                .contains("\"REVIEWER\"");
    }

    @Test
    void everyAuthenticatedIdentityIsAtLeastAReviewer() throws Exception {
        HttpResponse<String> response = get("/api/me", Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.body())
                .contains("\"REVIEWER\"")
                .doesNotContain("\"ARCHITECT\"")
                .doesNotContain("\"ADMIN\"");
    }

    /**
     * A check runner authenticates through the confidential client's service account. Keycloak
     * names that user {@code service-account-specforge-api}, and that prefix is what makes the
     * mirrored identity an agent — the field later capabilities read to keep agents from approving.
     */
    @Test
    void marksAServiceAccountAsAnAgent() throws Exception {
        HttpResponse<String> response = get("/api/me", Keycloak.serviceAccountToken());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"actorKind\":\"AGENT\"");
    }

    @Test
    void keepsOneRowPerSubjectAcrossRepeatedRequests() throws Exception {
        String token = Keycloak.passwordToken("admin-user", "admin-user");

        String first = get("/api/me", token).body();
        String second = get("/api/me", token).body();

        assertThat(first).isEqualTo(second);
    }
}
