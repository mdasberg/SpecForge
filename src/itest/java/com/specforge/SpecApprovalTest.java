package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.repository.forge.ReviewStatusState;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The approval capability end to end: the project's rule ("2 approvals, at least one architect")
 * is unsatisfied until an architect approves, an agent may never approve, a verdict cast against a
 * stale head is refused, a head advance resets approvals to pending, and the outcome reports back
 * onto the pull request through the same {@code ReviewStatusReporter} the repository capability
 * posts the initial pending status through.
 */
@Import(SpecApprovalTest.FakeForgeConfiguration.class)
@TestPropertySource(properties = "specforge.github.webhook-secret=itest-secret")
class SpecApprovalTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SECRET = "itest-secret";
    private static final String SPEC = """
            # Billing

            ## Purpose

            Bill things.
            """;

    @TestConfiguration
    static class FakeForgeConfiguration {

        @Bean
        @Primary
        FakeForge fakeForge() {
            return new FakeForge();
        }
    }

    @Autowired
    private FakeForge forge;

    @Autowired
    private JdbcClient jdbc;

    @LocalServerPort
    private int localPort;

    private UUID connectionId;

    @BeforeEach
    void reset() throws Exception {
        jdbc.sql("""
                TRUNCATE TABLE review_verdict, review_reviewer, thread, review, spec_diff, spec_search,
                    spec_section, spec_version, spec_document_tag, spec_document,
                    import_run_file, import_run, repository_scan_file, repository_scan,
                    spec_change_proposal_file, spec_change_proposal, repository_connection,
                    project_domain, project_required_role, project,
                    forge_installation_repository, forge_installation, webhook_delivery CASCADE
                """).update();
        forge.reset();
        forge.put("openspec/specs/billing/spec.md", SPEC);
        get("/api/me", architectToken());
        get("/api/me", reviewerToken());
        connectionId = connect();
        awaitLatestRun(connectionId);
    }

    @Test
    void theRuleIsUnsatisfiedUntilAnArchitectApprovesAndSatisfiedOnceOneDoes() throws Exception {
        String reviewId = openPullRequestReview(101, "sha-one");
        String headSha = headContentSha(reviewId);

        JsonNode afterReviewer = castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        assertThat(afterReviewer.get("rule").get("satisfied").asBoolean()).isFalse();
        assertThat(afterReviewer.get("rule").get("approvedCount").asInt()).isEqualTo(1);
        assertThat(afterReviewer.get("rule").get("reason").asString()).contains("architect");
        assertThat(afterReviewer.get("gate").get("passed").asBoolean()).isFalse();
        assertThat(specStatus()).isEqualTo("IN_REVIEW");

        JsonNode afterArchitect = castVerdict(reviewId, "APPROVE", headSha, architectToken());
        assertThat(afterArchitect.get("rule").get("satisfied").asBoolean()).isTrue();
        assertThat(afterArchitect.get("rule").get("approvedCount").asInt()).isEqualTo(2);
        assertThat(afterArchitect.get("gate").get("passed").asBoolean()).isTrue();
        assertThat(specStatus()).isEqualTo("APPROVED");
        assertThat(forge.statuses())
                .extracting(FakeForge.StatusCall::state)
                .containsExactly(ReviewStatusState.PENDING, ReviewStatusState.SUCCESS);
    }

    @Test
    void anAgentIdentityMayNeverApprove() throws Exception {
        String reviewId = openPullRequestReview(102, "sha-two");
        String headSha = headContentSha(reviewId);

        HttpResponse<String> response = postVerdict(reviewId, "APPROVE", headSha, Keycloak.serviceAccountToken());
        assertThat(response.statusCode()).isEqualTo(409);

        JsonNode agentComment = castVerdict(reviewId, "COMMENT", headSha, Keycloak.serviceAccountToken());
        assertThat(agentComment.get("verdicts").get(0).get("verdictType").asString()).isEqualTo("COMMENT");
        assertThat(agentComment.get("rule").get("approvedCount").asInt()).isZero();
    }

    @Test
    void aVerdictCastAgainstAStaleHeadIsRefused() throws Exception {
        String reviewId = openPullRequestReview(103, "sha-three");
        String staleSha = headContentSha(reviewId);

        forge.put("openspec/specs/billing/spec.md", SPEC + "\n## Scope\n\nEverything billing.\n");
        forge.pullRequest(103, List.of("openspec/specs/billing/spec.md"));
        webhook("pull_request", pullRequestPayload("synchronize", 103, "sha-three-b"));

        assertThat(postVerdict(reviewId, "APPROVE", staleSha, reviewerToken()).statusCode()).isEqualTo(409);
    }

    @Test
    void aHeadAdvanceResetsApprovalsToPendingAndOptionalVerdictsAreRecordedButDoNotCount() throws Exception {
        String reviewId = openPullRequestReview(104, "sha-four");
        String headSha = headContentSha(reviewId);
        castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        JsonNode both = castVerdict(reviewId, "APPROVE", headSha, architectToken());
        assertThat(both.get("gate").get("passed").asBoolean()).isTrue();

        forge.put("openspec/specs/billing/spec.md", SPEC + "\n## Scope\n\nEverything billing.\n");
        forge.pullRequest(104, List.of("openspec/specs/billing/spec.md"));
        webhook("pull_request", pullRequestPayload("synchronize", 104, "sha-four-b"));

        JsonNode afterAdvance = getApproval(reviewId, reviewerToken());
        assertThat(afterAdvance.get("rule").get("satisfied").asBoolean()).isFalse();
        assertThat(afterAdvance.get("rule").get("approvedCount").asInt()).isZero();
        assertThat(specStatus()).isEqualTo("IN_REVIEW");

        String newHeadSha = headContentSha(reviewId);
        castVerdict(reviewId, "APPROVE", newHeadSha, architectToken());
        // A third reviewer's approve has no compatible seat left (2 of 2 already claimed): optional.
        JsonNode optional = castVerdict(reviewId, "APPROVE", newHeadSha, adminToken());
        assertThat(optional.get("gate").get("passed").asBoolean()).isFalse();
        assertThat(optional.get("rule").get("approvedCount").asInt()).isEqualTo(1);
        assertThat(optional.get("verdicts").valueStream()
                        .filter(v -> v.get("verdictType").asString().equals("APPROVE"))
                        .count())
                .isEqualTo(2);
    }

    @Test
    void aRequiredReviewerRequestingChangesMovesTheSpecificationAndTheReport() throws Exception {
        String reviewId = openPullRequestReview(105, "sha-five");
        String headSha = headContentSha(reviewId);

        JsonNode after = castVerdict(reviewId, "REQUEST_CHANGES", headSha, architectToken());
        assertThat(after.get("reviewers").valueStream()
                        .anyMatch(r -> "CHANGES_REQUESTED".equals(r.get("state").asString())))
                .isTrue();
        assertThat(specStatus()).isEqualTo("CHANGES_REQUESTED");
        assertThat(forge.statuses())
                .extracting(FakeForge.StatusCall::state)
                .containsExactly(ReviewStatusState.PENDING, ReviewStatusState.FAILURE);
    }

    @Test
    void anUnresolvedBlockingThreadPreventsTheGateEvenWhenTheRuleIsSatisfied() throws Exception {
        String reviewId = openPullRequestReview(106, "sha-six");
        String headSha = headContentSha(reviewId);
        post("/api/reviews/" + reviewId + "/threads", """
                {"sectionKey": "purpose-1", "body": "Needs a decision."}
                """, reviewerToken());

        castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        JsonNode status = castVerdict(reviewId, "APPROVE", headSha, architectToken());

        assertThat(status.get("rule").get("satisfied").asBoolean()).isTrue();
        assertThat(status.get("unresolvedBlockingThreads").asInt()).isEqualTo(1);
        assertThat(status.get("gate").get("passed").asBoolean()).isFalse();
        assertThat(specStatus()).isEqualTo("IN_REVIEW");
    }

    @Test
    void anAdministratorAddsAndRemovesARequiredReviewer() throws Exception {
        String reviewId = openPullRequestReview(107, "sha-seven");
        String architectSubjectId = JSON.readTree(get("/api/me", architectToken()).body()).get("id").asString();

        HttpResponse<String> forbidden = post("/api/reviews/" + reviewId + "/required-reviewers", """
                {"subjectId": "%s"}
                """.formatted(architectSubjectId), reviewerToken());
        assertThat(forbidden.statusCode()).isEqualTo(403);

        JsonNode added = JSON.readTree(post("/api/reviews/" + reviewId + "/required-reviewers", """
                {"subjectId": "%s"}
                """.formatted(architectSubjectId), adminToken()).body());
        JsonNode manualSeat = added.get("reviewers").valueStream()
                .filter(r -> "MANUAL".equals(r.get("origin").asString()))
                .findFirst()
                .orElseThrow();
        assertThat(manualSeat.get("reviewer").get("subjectId").asString()).isEqualTo(architectSubjectId);

        HttpResponse<String> removed = delete(
                "/api/reviews/" + reviewId + "/required-reviewers/" + manualSeat.get("id").asString(), adminToken());
        assertThat(removed.statusCode()).isEqualTo(200);
        JsonNode afterRemoval = JSON.readTree(removed.body());
        assertThat(afterRemoval.get("reviewers").valueStream()
                        .noneMatch(r -> "MANUAL".equals(r.get("origin").asString())))
                .isTrue();
    }

    @Test
    void editingTheRuleTakesEffectOnAnOpenReviewImmediately() throws Exception {
        String reviewId = openPullRequestReview(108, "sha-eight");
        assertThat(getApproval(reviewId, reviewerToken()).get("rule").get("requiredCount").asInt()).isEqualTo(2);

        HttpResponse<String> updated = put("/api/projects/Billing/approval-rule", """
                {"minApprovals": 1, "requiredRoles": []}
                """, adminToken());
        assertThat(updated.statusCode()).isEqualTo(200);

        assertThat(getApproval(reviewId, reviewerToken()).get("rule").get("requiredCount").asInt()).isEqualTo(1);
        String headSha = headContentSha(reviewId);
        JsonNode afterOneApproval = castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        assertThat(afterOneApproval.get("gate").get("passed").asBoolean()).isTrue();
    }

    private String openPullRequestReview(int number, String headSha) throws Exception {
        forge.pullRequest(number, List.of("openspec/specs/billing/spec.md"));
        webhook("pull_request", pullRequestPayload("opened", number, headSha));
        JsonNode reviews = JSON.readTree(get("/api/reviews?state=OPEN", reviewerToken()).body());
        for (JsonNode item : reviews.get("items").valueStream().toList()) {
            if (item.get("pullRequest") != null && !item.get("pullRequest").isNull()
                    && item.get("pullRequest").get("number").asInt() == number) {
                return item.get("id").asString();
            }
        }
        throw new AssertionError("No open review for pull request " + number);
    }

    private String headContentSha(String reviewId) throws Exception {
        return JSON.readTree(get("/api/reviews/" + reviewId, reviewerToken()).body()).get("head").get("contentSha").asString();
    }

    private JsonNode getApproval(String reviewId, String bearerToken) throws Exception {
        HttpResponse<String> response = get("/api/reviews/" + reviewId + "/approval", bearerToken);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private JsonNode castVerdict(String reviewId, String verdictType, String headSha, String bearerToken) throws Exception {
        HttpResponse<String> response = postVerdict(reviewId, verdictType, headSha, bearerToken);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> postVerdict(String reviewId, String verdictType, String headSha, String bearerToken)
            throws Exception {
        return post("/api/reviews/" + reviewId + "/verdicts", """
                {"verdictType": "%s", "atHeadSha": "%s"}
                """.formatted(verdictType, headSha), bearerToken);
    }

    private String specStatus() {
        return jdbc.sql("select status from spec_document where path = 'openspec/specs/billing/spec.md'")
                .query(String.class)
                .single();
    }

    private UUID connect() throws Exception {
        install();
        HttpResponse<String> response = post("/api/repository-connections", connectionRequest(installationId()), adminToken());
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private String connectionRequest(UUID installationId) {
        return """
                {"installationId":"%s","repositoryFullName":"%s","branch":"main",
                 "pathGlob":"openspec/specs/**/spec.md","specFormat":"OPENSPEC","syncMode":"ON_PULL_REQUEST",
                 "project":{"name":"Billing","team":"Payments","domains":["billing"],
                            "tracker":"NONE","approvalRule":{"minApprovals":2,"requiredRoles":["ARCHITECT"]}}}
                """.formatted(installationId, forge.repository());
    }

    private void install() throws Exception {
        webhook("installation", """
                {"action":"created","installation":{"id":%s}}
                """.formatted(forge.installationId()));
    }

    private UUID installationId() throws Exception {
        JsonNode installations = JSON.readTree(get("/api/forge/installations", adminToken()).body());
        return UUID.fromString(installations.get("items").get(0).get("id").asString());
    }

    private JsonNode awaitLatestRun(UUID connectionId) throws Exception {
        return await(() -> {
            JsonNode runs = JSON.readTree(
                    get("/api/repository-connections/" + connectionId + "/import-runs", adminToken()).body());
            if (runs.get("items").isEmpty()) {
                return null;
            }
            JsonNode latest = runs.get("items").get(0);
            String status = latest.get("status").asString();
            return status.equals("RUNNING") ? null : latest;
        });
    }

    private static JsonNode await(ThrowingSupplier supplier) throws Exception {
        Duration limit = Duration.ofSeconds(20);
        long deadline = System.nanoTime() + limit.toNanos();
        JsonNode settled = null;
        while (System.nanoTime() < deadline) {
            settled = supplier.get();
            if (settled != null) {
                return settled;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Nothing settled within " + limit);
    }

    private interface ThrowingSupplier {
        JsonNode get() throws Exception;
    }

    private static String pullRequestPayload(String action, int number, String headSha) {
        return """
                {"action":"%s","number":%d,
                 "repository":{"full_name":"acme/billing-specs"},
                 "pull_request":{"number":%d,"title":"Bill differently","base":{"ref":"main"},
                                 "head":{"sha":"%s"},"user":{"login":"ada"}}}
                """.formatted(action, number, number, headSha);
    }

    private void webhook(String event, String payload) throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(uri("/api/webhooks/github"))
                        .header("Content-Type", "application/json")
                        .header("X-GitHub-Event", event)
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-Hub-Signature-256", sign(payload))
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(202);
    }

    private static String sign(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static String reviewerToken() {
        return Keycloak.passwordToken("reviewer", "reviewer");
    }

    private static String architectToken() {
        return Keycloak.passwordToken("architect", "architect");
    }

    private static String adminToken() {
        return Keycloak.passwordToken("admin-user", "admin-user");
    }

    private HttpResponse<String> post(String path, String body, String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> put(String path, String body, String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(String path, String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + localPort + path);
    }
}
