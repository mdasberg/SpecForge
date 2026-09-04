package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
import com.specforge.repository.ProposalClosed;
import com.specforge.repository.ProposedSpec;
import com.specforge.repository.SpecChangeProposed;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The review capability end to end: a proposed change opens a review, a further push moves its head,
 * and the diff a reviewer reads is the one the server computed and cached.
 *
 * <p>The proposal event is published inside a transaction rather than handed to the service, because
 * the wiring under test is precisely that the listener runs <em>after</em> that transaction commits.
 * Calling the service directly would prove the mapping and hide the thing most likely to break.
 */
class SpecReviewTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID CONNECTION = UUID.randomUUID();
    private static final String REPOSITORY = "acme/care-specs";
    private static final String CLAIMS_PATH = "openspec/specs/claims/spec.md";
    private static final String LEDGER_PATH = "openspec/specs/ledger/spec.md";

    private static final String CLAIMS_V1 = """
            # Claims

            ## Purpose

            Claims are settled after validation of the member's cover.

            ## Validation Rules

            The member must be active.
            The benefit must have remaining balance.
            """;

    /** One sentence rewritten inside Validation Rules; everything else is untouched. */
    private static final String CLAIMS_PROPOSED = CLAIMS_V1.replace(
            "The benefit must have remaining balance.", "The benefit must have a positive balance.");

    /** The same, plus a section the base never had. */
    private static final String CLAIMS_PROPOSED_AGAIN = CLAIMS_PROPOSED + """

            ## Domain Events

            ClaimSettled is published on settlement.
            """;

    private static final String LEDGER_V1 = """
            # Ledger

            ## Purpose

            Every movement is double entry.
            """;

    @Autowired
    private SpecCatalog catalog;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationEventPublisher events;

    @Autowired
    private TransactionTemplate transactions;

    @LocalServerPort
    private int localPort;

    private UUID claimsId;
    private UUID ledgerId;

    @BeforeEach
    void seed() {
        jdbc.sql("TRUNCATE TABLE review, spec_diff, spec_search, spec_section, spec_version, "
                        + "spec_document_tag, spec_document CASCADE")
                .update();
        claimsId = importSpec(CLAIMS_PATH, CLAIMS_V1, "c0ffee");
        ledgerId = importSpec(LEDGER_PATH, LEDGER_V1, "c0ffee");
    }

    @Test
    void aProposedChangeOpensOneReviewPerSpecification() throws Exception {
        propose(42, "feedbeef", "octocat", new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED));

        JsonNode list = JSON.readTree(get("/api/reviews?state=OPEN", token()).body());

        assertThat(list.get("total").asInt()).isEqualTo(1);
        JsonNode review = list.get("items").get(0);
        assertThat(review.get("spec").get("id").asString()).isEqualTo(claimsId.toString());
        assertThat(review.get("spec").get("status").asString()).isEqualTo("IN_REVIEW");
        assertThat(review.get("base").get("label").asString()).isEqualTo("v1");
        // The head is not a version: the repository has not accepted it, so it is labelled by the
        // pull request it arrived on.
        assertThat(review.get("head").get("label").asString()).isEqualTo("#42");
        assertThat(review.get("head").get("ordinal").isNull()).isTrue();
        assertThat(review.get("pullRequest").get("number").asInt()).isEqualTo(42);
    }

    @Test
    void aPullRequestTouchingTwoSpecificationsOpensTwoReviews() throws Exception {
        propose(43, "feedbeef", "octocat",
                new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED),
                new ProposedSpec(ledgerId, LEDGER_PATH, LEDGER_V1 + "\nA reversal is two entries.\n"));

        JsonNode list = JSON.readTree(get("/api/reviews?state=OPEN", token()).body());

        assertThat(list.get("total").asInt()).isEqualTo(2);
        assertThat(specIds(list)).containsExactlyInAnyOrder(claimsId.toString(), ledgerId.toString());
    }

    @Test
    void aFurtherPushUpdatesTheHeadInsteadOfOpeningASecondReview() throws Exception {
        propose(44, "feedbeef", "octocat", new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED));
        String reviewId = openReviewId();
        String firstHead = JSON.readTree(get("/api/reviews/" + reviewId, token()).body())
                .get("head").get("contentSha").asString();

        propose(44, "cafebabe", "octocat", new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED_AGAIN));

        JsonNode list = JSON.readTree(get("/api/reviews?state=OPEN", token()).body());
        assertThat(list.get("total").asInt()).isEqualTo(1);
        assertThat(list.get("items").get(0).get("id").asString()).isEqualTo(reviewId);

        JsonNode review = JSON.readTree(get("/api/reviews/" + reviewId, token()).body());
        assertThat(review.get("head").get("contentSha").asString()).isNotEqualTo(firstHead);
        assertThat(review.get("content").asString()).contains("ClaimSettled is published");
    }

    @Test
    void theDiffNamesTheChangedSectionAndWhoChangedIt() throws Exception {
        propose(45, "feedbeef", "octocat", new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED));

        JsonNode diff = JSON.readTree(get("/api/reviews/" + openReviewId() + "/diff", token()).body());

        assertThat(diff.get("summary").get("modifiedSections").asInt()).isEqualTo(1);
        assertThat(diff.get("summary").get("addedSections").asInt()).isZero();
        assertThat(diff.get("summary").get("removedSections").asInt()).isZero();
        assertThat(changed(diff)).containsExactly("validation-rules-1");

        JsonNode section = diff.get("sections").valueStream()
                .filter(each -> each.get("anchorKey").asString().equals("validation-rules-1"))
                .findFirst()
                .orElseThrow();
        assertThat(section.get("change").asString()).isEqualTo("MODIFIED");
        assertThat(section.get("author").asString()).isEqualTo("octocat");
        assertThat(section.get("lines").valueStream()
                        .filter(line -> line.get("type").asString().equals("ADDED"))
                        .map(line -> line.get("text").asString())
                        .toList())
                .containsExactly("The benefit must have a positive balance.");
        // The words that actually changed, so the client highlights inside the line it renders.
        assertThat(section.get("lines").valueStream()
                        .anyMatch(line -> !line.get("words").isEmpty()))
                .isTrue();
    }

    @Test
    void theSameContentIsDiffedOnceAndServedFromTheCache() throws Exception {
        propose(46, "feedbeef", "octocat", new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED));
        String path = "/api/reviews/" + openReviewId() + "/diff";

        String first = get(path, token()).body();
        assertThat(cachedDiffs()).isEqualTo(1);

        assertThat(get(path, token()).body()).isEqualTo(first);
        assertThat(cachedDiffs()).isEqualTo(1);
    }

    @Test
    void aReviewIsOpenedManuallyBetweenTwoVersionsAndOnlyOnce() throws Exception {
        importSpec(LEDGER_PATH, LEDGER_V1 + "\nA reversal is two entries.\n", "cafebabe");

        HttpResponse<String> created = post("/api/reviews", """
                {"specId": "%s", "baseVersion": 1, "headVersion": 2}""".formatted(ledgerId), token());

        assertThat(created.statusCode()).isEqualTo(201);
        JsonNode review = JSON.readTree(created.body());
        assertThat(review.get("base").get("label").asString()).isEqualTo("v1");
        assertThat(review.get("head").get("label").asString()).isEqualTo("v2");
        assertThat(review.get("openedBy").asString()).isNotBlank();
        assertThat(review.get("pullRequest").isNull()).isTrue();
        // The lifecycle moved through the state machine, not by this endpoint setting a column.
        assertThat(JSON.readTree(get("/api/specs/" + ledgerId, token()).body()).get("status").asString())
                .isEqualTo("IN_REVIEW");

        HttpResponse<String> second = post("/api/reviews", """
                {"specId": "%s", "baseVersion": 1, "headVersion": 2}""".formatted(ledgerId), token());
        assertThat(second.statusCode()).isEqualTo(409);
    }

    @Test
    void refusesAReviewOfAVersionAgainstItself() throws Exception {
        assertThat(post("/api/reviews", """
                {"specId": "%s", "baseVersion": 1, "headVersion": 1}""".formatted(ledgerId), token())
                .statusCode())
                .isEqualTo(422);
    }

    @Test
    void comparingTwoVersionsCreatesNoReview() throws Exception {
        importSpec(LEDGER_PATH, LEDGER_V1 + "\nA reversal is two entries.\n", "cafebabe");

        JsonNode diff = JSON.readTree(get("/api/specs/" + ledgerId + "/diff?base=1&head=2", token()).body());

        assertThat(diff.get("base").get("label").asString()).isEqualTo("v1");
        assertThat(diff.get("head").get("label").asString()).isEqualTo("v2");
        assertThat(diff.get("summary").get("changedLines").asInt()).isPositive();
        assertThat(JSON.readTree(get("/api/reviews", token()).body()).get("total").asInt()).isZero();
        assertThat(JSON.readTree(get("/api/specs/" + ledgerId, token()).body()).get("status").asString())
                .isEqualTo("DRAFT");
    }

    @Test
    void closingThePullRequestClosesTheReview() throws Exception {
        UUID proposalId = propose(47, "feedbeef", "octocat",
                new ProposedSpec(claimsId, CLAIMS_PATH, CLAIMS_PROPOSED));
        String reviewId = openReviewId();

        transactions.executeWithoutResult(status ->
                events.publishEvent(new ProposalClosed(proposalId, true, Instant.now())));

        assertThat(JSON.readTree(get("/api/reviews?state=OPEN", token()).body()).get("total").asInt()).isZero();
        assertThat(JSON.readTree(get("/api/reviews/" + reviewId, token()).body()).get("state").asString())
                .isEqualTo("CLOSED");
    }

    @Test
    void refusesAnUnauthenticatedCaller() throws Exception {
        assertThat(get("/api/reviews", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/reviews/" + UUID.randomUUID(), null).statusCode()).isEqualTo(401);
        assertThat(get("/api/specs/" + ledgerId + "/diff?base=1&head=1", null).statusCode()).isEqualTo(401);
    }

    private UUID importSpec(final String path, final String content, final String commitSha) {
        return catalog
                .importVersion(new SpecImport(CONNECTION, REPOSITORY, path, "Care Management", "claims",
                        "Payments", "ada", Set.of(), content, commitSha))
                .documentId();
    }

    /** Published inside a transaction, because the listener under test runs after that commits. */
    private UUID propose(final int number, final String headSha, final String author, final ProposedSpec... specs) {
        final UUID proposalId = UUID.randomUUID();
        transactions.executeWithoutResult(status -> events.publishEvent(new SpecChangeProposed(
                CONNECTION, proposalId, REPOSITORY, number, headSha, author, List.of(specs), Instant.now())));
        return proposalId;
    }

    private String openReviewId() throws Exception {
        return JSON.readTree(get("/api/reviews?state=OPEN", token()).body())
                .get("items").get(0).get("id").asString();
    }

    private int cachedDiffs() {
        return jdbc.sql("SELECT count(*) FROM spec_diff").query(Integer.class).single();
    }

    private static List<String> specIds(final JsonNode list) {
        return list.get("items").valueStream().map(item -> item.get("spec").get("id").asString()).toList();
    }

    private static List<String> changed(final JsonNode diff) {
        return diff.get("sections").valueStream()
                .filter(section -> !section.get("change").asString().equals("UNCHANGED"))
                .map(section -> section.get("anchorKey").asString())
                .toList();
    }

    private static String token() {
        return Keycloak.passwordToken("reviewer", "reviewer");
    }

    private HttpResponse<String> post(final String path, final String body, final String bearerToken)
            throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + localPort + path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
