package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

/**
 * The resource server's contract at the edge: who gets in, who does not, and what a refusal looks
 * like. Every token here is issued by the composed Keycloak.
 */
class AuthenticationTest extends BaseIntegrationTest {

    @Test
    void refusesAnonymousApiRequestWithProblemJson() throws Exception {
        HttpResponse<String> response = get("/api/me", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"status\":401").contains("\"instance\":\"/api/me\"");
    }

    @Test
    void refusesTokenThisRealmDidNotIssue() throws Exception {
        // Structurally a JWT, signed by nothing this issuer knows.
        String foreignToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJpbnRydWRlciJ9.not-a-valid-signature";

        HttpResponse<String> response = get("/api/me", foreignToken);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.body()).doesNotContain("intruder");
    }

    @Test
    void acceptsATokenThisRealmIssued() throws Exception {
        HttpResponse<String> response = get("/api/me", Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"displayName\"");
    }
}
