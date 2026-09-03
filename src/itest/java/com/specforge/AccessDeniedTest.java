package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A denial has to render as problem+json like every other error. No endpoint requires a role yet,
 * so the denial is raised by a test-only controller: the thing under test is the handler that
 * renders it, which is production code.
 */
@Import(AccessDeniedTest.DeniedEndpoint.class)
class AccessDeniedTest extends BaseIntegrationTest {

    @Test
    void rendersADenialAsProblemJson() throws Exception {
        HttpResponse<String> response = get("/api/denied", Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"status\":403").contains("\"instance\":\"/api/denied\"");
    }

    @TestConfiguration
    @RestController
    static class DeniedEndpoint {

        @GetMapping("/api/denied")
        String denied() {
            throw new AccessDeniedException("nope");
        }
    }
}
