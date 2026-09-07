package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

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
 * The automated-review capability end to end: a review's first head dispatches the project's checks,
 * a failed blocking check stops the approval gate while a failed advisory one does not, an agent
 * identity may comment but never approve or resolve a human's thread, and a further push stales the
 * previous runs while the dispositions a reviewer already made travel with the finding.
 *
 * <p>No model is configured in the suite, so the three model-backed checks report skipped — which is
 * itself the behaviour worth having: an unreachable model never reads as a clean review.
 */
@Import(SpecAutomatedReviewTest.FakeForgeConfiguration.class)
@TestPropertySource(properties = "specforge.github.webhook-secret=itest-secret")
class SpecAutomatedReviewTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String SECRET = "itest-secret";
    private static final String PATH = "openspec/specs/billing/spec.md";

    /** The base every review starts from: one requirement, properly covered by a scenario. */
    private static final String COVERED = """
            # Billing

            ## Purpose

            Bill members for what they used.

            ## Requirements

            ### Requirement: An invoice is issued once per billing period
            The system SHALL issue exactly one invoice per member per billing period.

            #### Scenario: Second issue in the same period
            - **WHEN** a second invoice is requested for a period that already has one
            - **THEN** the request is refused
            """;

    /** The same specification with the scenario deleted, which the acceptance-criteria check blocks on. */
    private static final String UNCOVERED = COVERED.replace("""
            #### Scenario: Second issue in the same period
            - **WHEN** a second invoice is requested for a period that already has one
            - **THEN** the request is refused
            """, "");

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

    @BeforeEach
    void reset() throws Exception {
        jdbc.sql("""
                TRUNCATE TABLE finding_disposition, finding, check_run, check_definition,
                    review_verdict, review_reviewer, thread, review, spec_diff, spec_search,
                    spec_section, spec_version, spec_document_tag, spec_document,
                    import_run_file, import_run, repository_scan_file, repository_scan,
                    spec_change_proposal_file, spec_change_proposal, repository_connection,
                    project_domain, project_required_role, project,
                    forge_installation_repository, forge_installation, webhook_delivery CASCADE
                """).update();
        forge.reset();
        forge.put(PATH, COVERED);
        get("/api/me", architectToken());
        get("/api/me", reviewerToken());
        awaitLatestRun(connect());
    }

    @Test
    void aReviewsFirstHeadDispatchesTheProjectsChecks() throws Exception {
        String reviewId = openReview(201, "sha-one", COVERED);

        JsonNode checks = awaitChecksSettled(reviewId);
        assertThat(checks.get("items").size()).isEqualTo(8);
        assertThat(checks.get("summary").get("configured").asBoolean()).isTrue();
        assertThat(checks.get("summary").get("totalCount").asInt()).isEqualTo(8);
        // No model is configured here, so those three report skipped rather than passed.
        assertThat(states(checks, "MODEL")).containsOnly("SKIPPED");
        assertThat(checks.get("items").valueStream()
                        .filter(run -> run.get("checkKey").asString().equals("acceptance-criteria"))
                        .findFirst().orElseThrow().get("state").asString())
                .isEqualTo("PASSED");
    }

    @Test
    void aFailedBlockingCheckStopsTheGateAndNamesTheCheck() throws Exception {
        String reviewId = openReview(202, "sha-two", UNCOVERED);
        JsonNode checks = awaitChecksSettled(reviewId);

        JsonNode acceptance = run(checks, "acceptance-criteria");
        assertThat(acceptance.get("state").asString()).isEqualTo("FAILED");
        assertThat(acceptance.get("blocking").asBoolean()).isTrue();
        assertThat(acceptance.get("findings").get(0).get("author").get("actorKind").asString()).isEqualTo("AGENT");

        String headSha = headContentSha(reviewId);
        castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        JsonNode approval = castVerdict(reviewId, "APPROVE", headSha, architectToken());

        assertThat(approval.get("rule").get("satisfied").asBoolean()).isTrue();
        assertThat(approval.get("checks").get("failed").asBoolean()).isTrue();
        assertThat(approval.get("gate").get("passed").asBoolean()).isFalse();
        assertThat(approval.get("gate").get("reasons").valueStream().map(JsonNode::asString).toList())
                .anyMatch(reason -> reason.contains("Acceptance criteria coverage"));
        assertThat(specStatus()).isEqualTo("IN_REVIEW");
    }

    @Test
    void aFailedAdvisoryCheckIsVisibleButNeverBlocks() throws Exception {
        String reviewId = openReview(203, "sha-three", COVERED);
        awaitChecksSettled(reviewId);

        // The glossary is the project's own configuration; setting it here is what an administration
        // screen will do later, and it is the cheapest way to make an advisory check actually fail.
        jdbc.sql("UPDATE check_definition SET configuration = :glossary WHERE check_key = 'terminology'")
                .param("glossary", "subscriber | member")
                .update();
        post("/api/reviews/" + reviewId + "/checks", "", reviewerToken());
        JsonNode checks = awaitChecksSettled(reviewId);

        JsonNode terminology = run(checks, "terminology");
        assertThat(terminology.get("state").asString()).isEqualTo("FAILED");
        assertThat(terminology.get("blocking").asBoolean()).isFalse();
        assertThat(checks.get("summary").get("failedCount").asInt()).isEqualTo(1);
        assertThat(checks.get("summary").get("failed").asBoolean()).isFalse();

        String headSha = headContentSha(reviewId);
        castVerdict(reviewId, "APPROVE", headSha, reviewerToken());
        JsonNode approval = castVerdict(reviewId, "APPROVE", headSha, architectToken());

        assertThat(approval.get("checks").get("failed").asBoolean()).isFalse();
        assertThat(approval.get("checks").get("failedCount").asInt()).isEqualTo(1);
        assertThat(approval.get("gate").get("passed").asBoolean()).isTrue();
        assertThat(specStatus()).isEqualTo("APPROVED");
    }

    @Test
    void anAgentMayCommentButMayNotApproveOrResolveAHumanThread() throws Exception {
        String reviewId = openReview(204, "sha-four", COVERED);
        String agentToken = Keycloak.serviceAccountToken();
        String headSha = headContentSha(reviewId);

        assertThat(postVerdict(reviewId, "APPROVE", headSha, agentToken).statusCode()).isEqualTo(409);
        assertThat(postVerdict(reviewId, "REQUEST_CHANGES", headSha, agentToken).statusCode()).isEqualTo(409);
        assertThat(postVerdict(reviewId, "COMMENT", headSha, agentToken).statusCode()).isEqualTo(200);

        String threadId = JSON.readTree(post("/api/reviews/" + reviewId + "/threads", """
                {"sectionKey": "purpose-1", "body": "Is this per member or per account?"}
                """, reviewerToken()).body()).get("id").asString();

        assertThat(post("/api/threads/" + threadId + "/resolve", "", agentToken).statusCode()).isEqualTo(409);
        JsonNode threads = JSON.readTree(get("/api/reviews/" + reviewId + "/threads", reviewerToken()).body());
        assertThat(threads.get("items").get(0).get("resolved").asBoolean()).isFalse();
        assertThat(threads.get("unresolvedBlockingCount").asInt()).isEqualTo(1);
    }

    @Test
    void anAgentMayNotDisposeOfAFinding() throws Exception {
        String reviewId = openReview(205, "sha-five", UNCOVERED);
        String findingId = firstFindingId(awaitChecksSettled(reviewId));

        assertThat(post("/api/findings/" + findingId + "/dismiss", "", Keycloak.serviceAccountToken()).statusCode())
                .isEqualTo(409);
    }

    @Test
    void acceptingAFindingOpensAThreadCarryingItsSuggestionAndChangesNoContent() throws Exception {
        String reviewId = openReview(206, "sha-six", UNCOVERED);
        String findingId = firstFindingId(awaitChecksSettled(reviewId));

        JsonNode accepted = JSON.readTree(
                post("/api/findings/" + findingId + "/accept", "", reviewerToken()).body());
        assertThat(accepted.get("disposition").get("kind").asString()).isEqualTo("ACCEPTED");
        assertThat(accepted.get("disposition").get("actor").get("actorKind").asString()).isEqualTo("HUMAN");

        String threadId = accepted.get("disposition").get("threadId").asString();
        JsonNode thread = JSON.readTree(get("/api/reviews/" + reviewId + "/threads", reviewerToken()).body())
                .get("items").valueStream()
                .filter(item -> item.get("id").asString().equals(threadId))
                .findFirst().orElseThrow();
        assertThat(thread.get("opener").get("actorKind").asString()).isEqualTo("HUMAN");
        assertThat(thread.get("comments").get(0).get("body").asString()).contains("**WHEN**");
        // The catalogue normalises content, so the head is compared against the normalised form —
        // the point of the assertion is that accepting a suggestion edited nothing at all.
        assertThat(headContent(reviewId)).isEqualTo(UNCOVERED.stripTrailing() + "\n");

        JsonNode undone = JSON.readTree(
                delete("/api/findings/" + findingId + "/disposition", reviewerToken()).body());
        assertThat(undone.get("disposition").isNull() || !undone.has("disposition")).isTrue();
    }

    @Test
    void aHeadAdvanceStalesThePreviousRunsAndCarriesTheDispositionsForward() throws Exception {
        String reviewId = openReview(207, "sha-seven", UNCOVERED);
        JsonNode before = awaitChecksSettled(reviewId);
        String dismissed = firstFindingId(before);
        post("/api/findings/" + dismissed + "/dismiss", "", reviewerToken());
        List<String> previousRunIds = before.get("items").valueStream().map(run -> run.get("id").asString()).toList();

        // A further push to the same pull request: the head moves, and the still-uncovered
        // requirement is found again by a new run.
        forge.put(PATH, UNCOVERED + "\n## Notes\n\nBilling runs nightly.\n");
        forge.pullRequest(207, List.of(PATH));
        webhook("pull_request", pullRequestPayload("synchronize", 207, "sha-seven-b"));

        JsonNode after = awaitChecksSettled(reviewId, previousRunIds);
        assertThat(after.get("items").valueStream().map(run -> run.get("id").asString()).toList())
                .doesNotContainAnyElementsOf(previousRunIds);
        assertThat(after.get("items").valueStream().map(run -> run.get("headLabel").asString()).toList())
                .containsOnly("#207");

        JsonNode acceptance = run(after, "acceptance-criteria");
        assertThat(acceptance.get("state").asString()).isEqualTo("FAILED");
        JsonNode carried = acceptance.get("findings").get(0);
        assertThat(carried.get("id").asString()).isNotEqualTo(dismissed);
        assertThat(carried.get("disposition").get("kind").asString()).isEqualTo("DISMISSED");

        // The runs against the superseded head are kept, not deleted — that is what makes "what did
        // the previous head say" answerable at all.
        assertThat(jdbc.sql("SELECT count(*) FROM check_run WHERE stale = true AND review_id = :id")
                        .param("id", UUID.fromString(reviewId))
                        .query(Long.class)
                        .single())
                .isEqualTo(previousRunIds.size());
    }

    private JsonNode run(final JsonNode checks, final String checkKey) {
        return checks.get("items").valueStream()
                .filter(item -> item.get("checkKey").asString().equals(checkKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No run for check " + checkKey));
    }

    private List<String> states(final JsonNode checks, final String runnerKind) {
        return checks.get("items").valueStream()
                .filter(item -> item.get("runnerKind").asString().equals(runnerKind))
                .map(item -> item.get("state").asString())
                .toList();
    }

    private String firstFindingId(final JsonNode checks) {
        return run(checks, "acceptance-criteria").get("findings").get(0).get("id").asString();
    }

    /** Waits until no run is still queued or running, which is when the tab is worth asserting on. */
    private JsonNode awaitChecksSettled(final String reviewId) throws Exception {
        return awaitChecksSettled(reviewId, List.of());
    }

    private JsonNode awaitChecksSettled(final String reviewId, final List<String> notThese) throws Exception {
        return await(() -> {
            JsonNode checks = JSON.readTree(get("/api/reviews/" + reviewId + "/checks", reviewerToken()).body());
            if (checks.get("items").isEmpty()) {
                return null;
            }
            boolean superseded = checks.get("items").valueStream()
                    .anyMatch(run -> notThese.contains(run.get("id").asString()));
            boolean pending = checks.get("summary").get("pendingCount").asInt() > 0;
            return superseded || pending ? null : checks;
        });
    }

    private String openReview(final int number, final String headSha, final String content) throws Exception {
        forge.put(PATH, content);
        forge.pullRequest(number, List.of(PATH));
        webhook("pull_request", pullRequestPayload("opened", number, headSha));
        return await(() -> {
            JsonNode reviews = JSON.readTree(get("/api/reviews?state=OPEN", reviewerToken()).body());
            return reviews.get("items").valueStream()
                    .filter(item -> !item.get("pullRequest").isNull()
                            && item.get("pullRequest").get("number").asInt() == number)
                    .findFirst()
                    .orElse(null);
        }).get("id").asString();
    }

    private String headContentSha(final String reviewId) throws Exception {
        return JSON.readTree(get("/api/reviews/" + reviewId, reviewerToken()).body())
                .get("head").get("contentSha").asString();
    }

    private String headContent(final String reviewId) throws Exception {
        return JSON.readTree(get("/api/reviews/" + reviewId, reviewerToken()).body()).get("content").asString();
    }

    private JsonNode castVerdict(
            final String reviewId, final String verdictType, final String headSha, final String bearerToken)
            throws Exception {
        HttpResponse<String> response = postVerdict(reviewId, verdictType, headSha, bearerToken);
        assertThat(response.statusCode()).isEqualTo(200);
        return JSON.readTree(response.body());
    }

    private HttpResponse<String> postVerdict(
            final String reviewId, final String verdictType, final String headSha, final String bearerToken)
            throws Exception {
        return post("/api/reviews/" + reviewId + "/verdicts", """
                {"verdictType": "%s", "atHeadSha": "%s"}
                """.formatted(verdictType, headSha), bearerToken);
    }

    private String specStatus() {
        return jdbc.sql("select status from spec_document where path = :path")
                .param("path", PATH)
                .query(String.class)
                .single();
    }

    private UUID connect() throws Exception {
        webhook("installation", """
                {"action":"created","installation":{"id":%s}}
                """.formatted(forge.installationId()));
        JsonNode installations = JSON.readTree(get("/api/forge/installations", adminToken()).body());
        UUID installationId = UUID.fromString(installations.get("items").get(0).get("id").asString());
        HttpResponse<String> response = post("/api/repository-connections", """
                {"installationId":"%s","repositoryFullName":"%s","branch":"main",
                 "pathGlob":"openspec/specs/**/spec.md","specFormat":"OPENSPEC","syncMode":"ON_PULL_REQUEST",
                 "project":{"name":"Billing","team":"Payments","domains":["billing"],
                            "tracker":"NONE","approvalRule":{"minApprovals":2,"requiredRoles":["ARCHITECT"]}}}
                """.formatted(installationId, forge.repository()), adminToken());
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private JsonNode awaitLatestRun(final UUID connectionId) throws Exception {
        return await(() -> {
            JsonNode runs = JSON.readTree(
                    get("/api/repository-connections/" + connectionId + "/import-runs", adminToken()).body());
            if (runs.get("items").isEmpty()) {
                return null;
            }
            JsonNode latest = runs.get("items").get(0);
            return latest.get("status").asString().equals("RUNNING") ? null : latest;
        });
    }

    private static JsonNode await(final ThrowingSupplier supplier) throws Exception {
        Duration limit = Duration.ofSeconds(20);
        long deadline = System.nanoTime() + limit.toNanos();
        while (System.nanoTime() < deadline) {
            JsonNode settled = supplier.get();
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

    private static String pullRequestPayload(final String action, final int number, final String headSha) {
        return """
                {"action":"%s","number":%d,
                 "repository":{"full_name":"acme/billing-specs"},
                 "pull_request":{"number":%d,"title":"Bill differently","base":{"ref":"main"},
                                 "head":{"sha":"%s"},"user":{"login":"ada"}}}
                """.formatted(action, number, number, headSha);
    }

    private void webhook(final String event, final String payload) throws Exception {
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

    private static String sign(final String payload) throws Exception {
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

    private HttpResponse<String> post(final String path, final String body, final String bearerToken)
            throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> delete(final String path, final String bearerToken) throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(uri(path))
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .DELETE()
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(final String path) {
        return URI.create("http://localhost:" + localPort + path);
    }
}
