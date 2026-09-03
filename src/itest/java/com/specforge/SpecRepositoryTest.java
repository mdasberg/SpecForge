package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.repository.ReviewOutcome;
import com.specforge.repository.ReviewStatusReporter;
import com.specforge.repository.SpecChangeProposed;
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
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Import and synchronisation end to end, against a forge that answers from memory. Everything the
 * capability promises happens here through the API a client would use: install, scan, connect,
 * re-import, propose a change, lose access.
 */
@Import(SpecRepositoryTest.FakeForgeConfiguration.class)
@TestPropertySource(properties = "specforge.github.webhook-secret=itest-secret")
class SpecRepositoryTest extends BaseIntegrationTest {

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

        @Bean
        ProposedChanges proposedChanges() {
            return new ProposedChanges();
        }
    }

    /** Records the event the review capability will consume, so its emission is asserted, not assumed. */
    static class ProposedChanges {

        private final List<SpecChangeProposed> events = new java.util.concurrent.CopyOnWriteArrayList<>();

        @EventListener
        void on(SpecChangeProposed event) {
            events.add(event);
        }

        List<SpecChangeProposed> all() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }

    @Autowired
    private FakeForge forge;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ProposedChanges proposedChanges;

    @Autowired
    private ReviewStatusReporter reviewStatus;

    @LocalServerPort
    private int localPort;

    @BeforeEach
    void reset() {
        jdbc.sql("""
                TRUNCATE TABLE spec_section, spec_version, spec_document_tag, spec_document,
                    import_run_file, import_run, repository_scan_file, repository_scan,
                    spec_change_proposal_file, spec_change_proposal, repository_connection,
                    project_domain, project_required_role, project,
                    forge_installation_repository, forge_installation, webhook_delivery CASCADE
                """).update();
        forge.reset();
        proposedChanges.clear();
        forge.put("openspec/specs/billing/spec.md", SPEC);
        forge.put("openspec/specs/care/spec.md", "---\ntags: [care, claims]\n---\n# Care\n\n## Purpose\n\nCare.\n");
        forge.put("openspec/changes/add-billing/specs/billing/spec.md", "# Proposed\n");
        forge.put("openspec/specs/broken/spec.md", "no heading here\n");
        forge.put("README.md", "# Not a spec\n");
    }

    @Test
    void classifiesEveryMatchedFileBeforeAnythingIsConnected() throws Exception {
        install();
        UUID installationId = installationId();

        JsonNode scan = awaitScan(startScan(installationId));

        assertThat(scan.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(scan.get("importableCount").asInt()).isEqualTo(2);
        assertThat(scan.get("changeProposalCount").asInt()).isZero();
        assertThat(scan.get("unparsableCount").asInt()).isEqualTo(1);
        assertThat(scan.get("files").toString()).contains("no heading");
    }

    @Test
    void importsEverySpecificationOnTheInitialImport() throws Exception {
        UUID connectionId = connect();

        JsonNode run = awaitLatestRun(connectionId);

        assertThat(run.get("trigger").asString()).isEqualTo("INITIAL");
        assertThat(run.get("status").asString()).isEqualTo("SUCCEEDED");
        assertThat(run.get("importedCount").asInt()).isEqualTo(2);
        assertThat(documentCount()).isEqualTo(2);
        assertThat(versionCount()).isEqualTo(2);
    }

    @Test
    void createsNoVersionWhenNothingChanged() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);

        forge.commit("c0ffee2");
        JsonNode run = awaitLatestRun(startImport(connectionId));

        assertThat(run.get("unchangedCount").asInt()).isEqualTo(2);
        assertThat(run.get("importedCount").asInt()).isZero();
        assertThat(versionCount()).isEqualTo(2);
    }

    @Test
    void createsTheNextVersionWhenContentChanged() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);

        forge.put("openspec/specs/billing/spec.md", SPEC + "\n## Preconditions\n\nA policy exists.\n");
        forge.commit("c0ffee3");
        JsonNode run = awaitLatestRun(startImport(connectionId));

        assertThat(run.get("importedCount").asInt()).isEqualTo(1);
        assertThat(run.get("unchangedCount").asInt()).isEqualTo(1);
        assertThat(versionCount()).isEqualTo(3);
        assertThat(jdbc.sql("select max(ordinal) from spec_version v join spec_document d on d.id = v.document_id"
                        + " where d.path = 'openspec/specs/billing/spec.md'")
                        .query(Integer.class)
                        .single())
                .isEqualTo(2);
    }

    @Test
    void derivesSpecificationMetadataFromTheConnectionAndThePath() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);

        assertThat(jdbc.sql("""
                        select title || '|' || project || '|' || domain || '|' || owner || '|' || owning_team
                        from spec_document where path = 'openspec/specs/care/spec.md'
                        """)
                        .query(String.class)
                        .single())
                .isEqualTo("Care|Billing|care|ada|Payments");
        assertThat(jdbc.sql("""
                        select tag from spec_document_tag t
                        join spec_document d on d.id = t.document_id
                        where d.path = 'openspec/specs/care/spec.md' order by tag
                        """)
                        .query(String.class)
                        .list())
                .containsExactly("care", "claims");
    }

    @Test
    void storesTheProjectConfigurationCapturedInTheWizard() throws Exception {
        connect();

        assertThat(jdbc.sql("select name || '|' || team || '|' || min_approvals from project")
                        .query(String.class)
                        .single())
                .isEqualTo("Billing|Payments|2");
        assertThat(jdbc.sql("select role from project_required_role").query(String.class).list())
                .containsExactly("ARCHITECT");
        assertThat(jdbc.sql("select domain from project_domain").query(String.class).list())
                .containsExactly("billing");
    }

    @Test
    void reportsTheReviewVerdictBackOntoThePullRequest() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);
        forge.pullRequest(42, List.of("openspec/specs/billing/spec.md"));
        webhook("pull_request", pullRequestPayload("opened", 42, "sha-one"));
        UUID proposalId = proposedChanges.all().getFirst().proposalId();

        reviewStatus.report(proposalId, ReviewOutcome.APPROVED, "Approved by two architects");

        assertThat(forge.statuses())
                .extracting(FakeForge.StatusCall::state)
                .containsExactly(ReviewStatusState.PENDING, ReviewStatusState.SUCCESS);
    }

    @Test
    void recordsAPullRequestAsAProposedChangeAndReportsItPending() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);
        forge.pullRequest(42, List.of("openspec/specs/billing/spec.md"));
        forge.commit("feedbeef");

        webhook("pull_request", """
                {"action":"opened","number":42,
                 "repository":{"full_name":"%s"},
                 "pull_request":{"number":42,"title":"Bill differently","base":{"ref":"main"},
                                 "head":{"sha":"feedbeef"},"user":{"login":"ada"}}}
                """.formatted(forge.repository()));

        assertThat(jdbc.sql("select count(*) from spec_change_proposal where pull_request_number = 42")
                        .query(Integer.class)
                        .single())
                .isEqualTo(1);
        assertThat(jdbc.sql("select status from spec_document where path = 'openspec/specs/billing/spec.md'")
                        .query(String.class)
                        .single())
                .isEqualTo("IN_REVIEW");
        assertThat(forge.statuses())
                .extracting(FakeForge.StatusCall::state)
                .containsExactly(ReviewStatusState.PENDING);
        assertThat(proposedChanges.all())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.pullRequestNumber()).isEqualTo(42);
                    assertThat(event.headSha()).isEqualTo("feedbeef");
                    assertThat(event.documentIds()).hasSize(1);
                });
    }

    @Test
    void replacesTheProposalHeadWhenThePullRequestIsUpdated() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);
        forge.pullRequest(42, List.of("openspec/specs/billing/spec.md"));

        webhook("pull_request", pullRequestPayload("opened", 42, "sha-one"));
        webhook("pull_request", pullRequestPayload("synchronize", 42, "sha-two"));

        assertThat(jdbc.sql("select count(*) from spec_change_proposal").query(Integer.class).single())
                .isEqualTo(1);
        assertThat(jdbc.sql("select head_sha from spec_change_proposal").query(String.class).single())
                .isEqualTo("sha-two");
    }

    @Test
    void degradesTheConnectionWhenTheInstallationIsRevoked() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);

        forge.uninstall();
        webhook("installation", """
                {"action":"deleted","installation":{"id":%s}}
                """.formatted(forge.installationId()));

        JsonNode connection = JSON.readTree(
                get("/api/repository-connections/" + connectionId, token()).body());
        assertThat(connection.get("state").asString()).isEqualTo("DEGRADED");
        assertThat(connection.get("degradedReason").asString()).contains("revoked");
        // Everything imported before access was lost is still readable.
        assertThat(documentCount()).isEqualTo(2);

        HttpResponse<String> refused = post("/api/repository-connections/" + connectionId + "/imports", "", token());
        assertThat(refused.statusCode()).isEqualTo(409);
    }

    @Test
    void refusesASecondConnectionToTheSameRepositoryBranchAndPath() throws Exception {
        connect();

        HttpResponse<String> response = post("/api/repository-connections", connectionRequest(installationId()), token());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body()).contains("already connected");
    }

    @Test
    void refusesAConnectionWhoseGlobMatchesNothing() throws Exception {
        install();

        HttpResponse<String> response = post(
                "/api/repository-connections",
                connectionRequest(installationId()).replace("openspec/specs/**/spec.md", "nothing/**/spec.md"),
                token());

        assertThat(response.statusCode()).isEqualTo(422);
        assertThat(response.body()).contains("No specification was found");
    }

    @Test
    void refusesToEditSpecificationContentAndNamesTheRepository() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);
        UUID specId = UUID.fromString(jdbc.sql(
                        "select id::text from spec_document where path = 'openspec/specs/billing/spec.md'")
                .query(String.class)
                .single());

        HttpResponse<String> response = put(
                "/api/specs/" + specId + "/content", "{\"content\":\"# Edited\"}", token());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.body())
                .contains(forge.repository())
                .contains("openspec/specs/billing/spec.md");
    }

    @Test
    void acknowledgesARedeliveredWebhookWithoutActingOnItTwice() throws Exception {
        UUID connectionId = connect();
        awaitLatestRun(connectionId);
        forge.pullRequest(42, List.of("openspec/specs/billing/spec.md"));
        String payload = pullRequestPayload("opened", 42, "sha-one");

        webhookWithDelivery("pull_request", payload, "delivery-1");
        webhookWithDelivery("pull_request", payload, "delivery-1");

        assertThat(forge.statuses()).hasSize(1);
    }

    @Test
    void rejectsAWebhookWhoseSignatureDoesNotVerify() throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(uri("/api/webhooks/github"))
                        .header("Content-Type", "application/json")
                        .header("X-GitHub-Event", "push")
                        .header("X-GitHub-Delivery", UUID.randomUUID().toString())
                        .header("X-Hub-Signature-256", "sha256=" + "0".repeat(64))
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void versionsTheBranchOnAPushWhenTheConnectionSyncsOnPush() throws Exception {
        UUID connectionId = connect("ON_PUSH");
        awaitLatestRun(connectionId);
        forge.put("openspec/specs/billing/spec.md", SPEC + "\n## Preconditions\n\nA policy exists.\n");
        forge.commit("pushed1");

        webhook("push", """
                {"ref":"refs/heads/main","repository":{"full_name":"%s"},
                 "commits":[{"added":[],"modified":["openspec/specs/billing/spec.md"]}]}
                """.formatted(forge.repository()));

        JsonNode run = awaitLatestRun(connectionId);
        assertThat(run.get("trigger").asString()).isEqualTo("PUSH");
        assertThat(run.get("importedCount").asInt()).isEqualTo(1);
        assertThat(jdbc.sql("select count(*) from spec_change_proposal").query(Integer.class).single())
                .isZero();
    }

    // --- helpers -------------------------------------------------------------------------------

    private static String token() {
        return Keycloak.passwordToken("admin-user", "admin-user");
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + localPort + path);
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

    private void webhook(String event, String payload) throws Exception {
        webhookWithDelivery(event, payload, UUID.randomUUID().toString());
    }

    private void webhookWithDelivery(String event, String payload, String deliveryId) throws Exception {
        HttpResponse<String> response = HTTP.send(
                HttpRequest.newBuilder(uri("/api/webhooks/github"))
                        .header("Content-Type", "application/json")
                        .header("X-GitHub-Event", event)
                        .header("X-GitHub-Delivery", deliveryId)
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

    private void install() throws Exception {
        webhook("installation", """
                {"action":"created","installation":{"id":%s}}
                """.formatted(forge.installationId()));
    }

    private UUID installationId() throws Exception {
        JsonNode installations = JSON.readTree(get("/api/forge/installations", token()).body());
        return UUID.fromString(installations.get("items").get(0).get("id").asString());
    }

    private UUID connect() throws Exception {
        return connect("ON_PULL_REQUEST");
    }

    private UUID connect(String syncMode) throws Exception {
        install();
        HttpResponse<String> response =
                post("/api/repository-connections", connectionRequest(installationId(), syncMode), token());
        assertThat(response.statusCode()).isEqualTo(201);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private String connectionRequest(UUID installationId) {
        return connectionRequest(installationId, "ON_PULL_REQUEST");
    }

    private String connectionRequest(UUID installationId, String syncMode) {
        return """
                {"installationId":"%s","repositoryFullName":"%s","branch":"main",
                 "pathGlob":"openspec/specs/**/spec.md","specFormat":"OPENSPEC","syncMode":"%s",
                 "project":{"name":"Billing","team":"Payments","domains":["billing"],
                            "tracker":"NONE","approvalRule":{"minApprovals":2,"requiredRoles":["ARCHITECT"]}}}
                """.formatted(installationId, forge.repository(), syncMode);
    }

    private UUID startScan(UUID installationId) throws Exception {
        HttpResponse<String> response = post("/api/forge/scans", """
                {"installationId":"%s","repositoryFullName":"%s","branch":"main",
                 "pathGlob":"openspec/specs/**/spec.md","specFormat":"OPENSPEC"}
                """.formatted(installationId, forge.repository()), token());
        assertThat(response.statusCode()).isEqualTo(202);
        return UUID.fromString(JSON.readTree(response.body()).get("id").asString());
    }

    private UUID startImport(UUID connectionId) throws Exception {
        HttpResponse<String> response = post("/api/repository-connections/" + connectionId + "/imports", "", token());
        assertThat(response.statusCode()).isEqualTo(202);
        return connectionId;
    }

    private JsonNode awaitScan(UUID scanId) throws Exception {
        return await(() -> {
            JsonNode scan = JSON.readTree(get("/api/forge/scans/" + scanId, token()).body());
            String status = scan.get("status").asString();
            return status.equals("SUCCEEDED") || status.equals("FAILED") ? scan : null;
        });
    }

    private JsonNode awaitLatestRun(UUID connectionId) throws Exception {
        return await(() -> {
            JsonNode runs = JSON.readTree(
                    get("/api/repository-connections/" + connectionId + "/import-runs", token()).body());
            if (runs.get("items").isEmpty()) {
                return null;
            }
            JsonNode latest = runs.get("items").get(0);
            String status = latest.get("status").asString();
            return status.equals("RUNNING") ? null : latest;
        });
    }

    /** Scans and imports run asynchronously, so the assertion is on the settled result, not a race. */
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

    private int documentCount() {
        return jdbc.sql("select count(*) from spec_document").query(Integer.class).single();
    }

    private int versionCount() {
        return jdbc.sql("select count(*) from spec_version").query(Integer.class).single();
    }
}
