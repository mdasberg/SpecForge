package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The shape of an error is part of the API contract, so it is asserted rather than assumed: a
 * validation failure, an unexpected failure and the generated OpenAPI document all have to behave
 * the way a client is told they do.
 */
@Import(ApiConventionsTest.ConventionEndpoints.class)
class ApiConventionsTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int localPort;

    @Test
    void rendersAValidationFailureAsProblemJsonNamingTheField() throws Exception {
        HttpResponse<String> response = post("/api/conventions/echo", "{\"name\":\"\"}",
                Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(response.body()).contains("\"errors\"").contains("\"name\"");
    }

    @Test
    void rendersAnUnexpectedFailureWithoutLeakingItsCause() throws Exception {
        HttpResponse<String> response = get("/api/conventions/boom", Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                contentType -> assertThat(contentType).startsWith("application/problem+json"));
        assertThat(response.body())
                .contains("Something went wrong.")
                .doesNotContain("the database password is hunter2");
    }

    @Test
    void servesTheContractTheServerInterfacesWereGeneratedFrom() throws Exception {
        HttpResponse<String> response = get("/api/openapi.json", Keycloak.passwordToken("reviewer", "reviewer"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"openapi\"").contains("/api/me");
    }

    /**
     * The contract is only worth serving if the implementation actually honours it, so the schema
     * is read from the served document and checked against a real response rather than assumed.
     */
    @Test
    void answersWithEveryFieldTheContractDeclaresRequired() throws Exception {
        String token = Keycloak.passwordToken("architect", "architect");
        JsonNode contract = JSON.readTree(get("/api/openapi.json", token).body());
        JsonNode identity = JSON.readTree(get("/api/me", token).body());

        JsonNode required = contract.get("components").get("schemas").get("Identity").get("required");
        assertThat(required).isNotEmpty();
        for (JsonNode field : required) {
            assertThat(identity.has(field.asString()))
                    .as("contract requires Identity.%s", field.asString())
                    .isTrue();
        }
    }

    @Test
    void refusesTheOpenApiDocumentWithoutAToken() throws Exception {
        assertThat(get("/api/openapi.json", null).statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> post(String path, String body, String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + localPort + path))
                        .header("Authorization", "Bearer " + bearerToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    @RestController
    static class ConventionEndpoints {

        @PostMapping("/api/conventions/echo")
        String echo(@Valid @RequestBody Echo echo) {
            return echo.name();
        }

        @org.springframework.web.bind.annotation.GetMapping("/api/conventions/boom")
        String boom() {
            throw new IllegalStateException("the database password is hunter2");
        }

        record Echo(@NotBlank String name) {
        }
    }
}
