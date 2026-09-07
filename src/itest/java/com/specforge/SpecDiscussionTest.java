package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
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
 * The discussion capability end to end: opening a thread anchors it against the review's real head,
 * a reply raises the notifications the mention and the reply rules promise, resolving and reopening
 * move the review's unresolved count, and a further push carries every live anchor forward through
 * the same {@code ReviewHeadAdvanced} event the review capability publishes.
 */
class SpecDiscussionTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID CONNECTION = UUID.randomUUID();
    private static final String REPOSITORY = "acme/care-specs";
    private static final String CLAIMS_PATH = "openspec/specs/claims/spec.md";
    private static final int PR_NUMBER = 100;

    private static final String CLAIMS_V1 = """
            # Claims

            ## Purpose

            Claims are settled after validation of the member's cover.

            ## Validation Rules

            The member must be active.
            The benefit must have remaining balance.

            ## Notes

            Nothing else to say.
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
    private String reviewId;

    @BeforeEach
    void seed() throws Exception {
        jdbc.sql("TRUNCATE TABLE thread, review, spec_diff, spec_search, spec_section, spec_version, "
                        + "spec_document_tag, spec_document CASCADE")
                .update();
        claimsId = catalog
                .importVersion(new SpecImport(CONNECTION, REPOSITORY, CLAIMS_PATH, "Care Management", "claims",
                        "Payments", "ada", Set.of(), CLAIMS_V1, "c0ffee"))
                .documentId();
        propose(CLAIMS_V1, "feedbeef", "octocat");
        // @-mentions only resolve against a member who has been mirrored at least once; every test
        // that mentions @architect needs that row to already exist.
        get("/api/me", architectToken());
        reviewId = JSON.readTree(get("/api/reviews?state=OPEN", reviewerToken()).body())
                .get("items").get(0).get("id").asString();
    }

    @Test
    void openingASectionThreadAnchorsAgainstTheCurrentHeadAndQuotesItsHeading() throws Exception {
        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "The 409 body should carry the conflicting id. cc @architect"}
                """, reviewerToken());

        assertThat(thread.get("sectionKey").asString()).isEqualTo("validation-rules-1");
        assertThat(thread.get("quotedText").asString()).isEqualTo("Validation Rules");
        assertThat(thread.get("anchorState").asString()).isEqualTo("CURRENT");
        assertThat(thread.get("anchorVersionLabel").asString()).isEqualTo("#" + PR_NUMBER);
        assertThat(thread.get("blocking").asBoolean()).isTrue();
        assertThat(thread.get("resolved").asBoolean()).isFalse();
        assertThat(thread.get("opener").get("actorKind").asString()).isEqualTo("HUMAN");

        JsonNode opening = thread.get("comments").get(0);
        assertThat(opening.get("author").get("actorKind").asString()).isEqualTo("HUMAN");
        assertThat(opening.get("mentions").valueStream().map(JsonNode::asString).toList()).containsExactly("architect");
    }

    @Test
    void aMentionNotifiesTheMentionedMemberAndAReplyNotifiesTheOpener() throws Exception {
        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "cc @architect, does this need a 409?"}
                """, reviewerToken());
        String threadId = thread.get("id").asString();

        JsonNode architectInbox = JSON.readTree(get("/api/notifications", architectToken()).body());
        assertThat(architectInbox.get("unreadCount").asInt()).isEqualTo(1);
        assertThat(architectInbox.get("items").get(0).get("type").asString()).isEqualTo("MENTION");

        post("/api/threads/" + threadId + "/comments", """
                {"body": "Yes, and it should carry the conflicting pre-authorization id."}
                """, architectToken());

        JsonNode reviewerInbox = JSON.readTree(get("/api/notifications", reviewerToken()).body());
        assertThat(reviewerInbox.get("items").valueStream()
                        .anyMatch(n -> n.get("type").asString().equals("REPLY")))
                .isTrue();

        String notificationId = reviewerInbox.get("items").get(0).get("id").asString();
        HttpResponse<String> read = post("/api/notifications/" + notificationId + "/read", "", reviewerToken());
        assertThat(JSON.readTree(read.body()).get("readAt").isNull()).isFalse();
    }

    @Test
    void mentioningAHandleThatIsNotAMemberNotifiesNoOne() throws Exception {
        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "cc @not-a-real-member"}
                """, reviewerToken());

        assertThat(thread.get("comments").get(0).get("mentions").isEmpty()).isTrue();
    }

    @Test
    void resolvingAndReopeningMoveTheReviewsUnresolvedCount() throws Exception {
        String threadId = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "Needs a decision."}
                """, reviewerToken()).get("id").asString();

        assertThat(unresolvedBlocking(reviewId)).isEqualTo(1);

        JsonNode resolved = JSON.readTree(post("/api/threads/" + threadId + "/resolve", "", reviewerToken()).body());
        assertThat(resolved.get("resolved").asBoolean()).isTrue();
        assertThat(resolved.get("resolvedBy").get("handle").asString()).isEqualTo("reviewer");
        assertThat(unresolvedBlocking(reviewId)).isZero();

        assertThat(post("/api/threads/" + threadId + "/resolve", "", reviewerToken()).statusCode()).isEqualTo(409);

        JsonNode reopened = JSON.readTree(post("/api/threads/" + threadId + "/reopen", "", reviewerToken()).body());
        assertThat(reopened.get("resolved").asBoolean()).isFalse();
        assertThat(reopened.get("reopenedBy").get("handle").asString()).isEqualTo("reviewer");
        assertThat(unresolvedBlocking(reviewId)).isEqualTo(1);
    }

    @Test
    void anAgentThreadNeverBlocksAnApproval() throws Exception {
        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "CM-C-107 has no acceptance criterion."}
                """, Keycloak.serviceAccountToken());

        assertThat(thread.get("opener").get("actorKind").asString()).isEqualTo("AGENT");
        assertThat(thread.get("blocking").asBoolean()).isFalse();
        assertThat(thread.get("comments").get(0).get("author").get("actorKind").asString()).isEqualTo("AGENT");

        JsonNode list = JSON.readTree(get("/api/reviews/" + reviewId + "/threads", reviewerToken()).body());
        assertThat(list.get("unresolvedNonBlockingCount").asInt()).isEqualTo(1);
        assertThat(list.get("unresolvedBlockingCount").asInt()).isZero();
    }

    @Test
    void aThreadOnASelectedPhraseQuotesExactlyThatPhrase() throws Exception {
        JsonNode headed = JSON.readTree(get("/api/reviews/" + reviewId, reviewerToken()).body());
        String content = headed.get("content").asString();
        String phrase = "The member must be active.";
        int start = content.indexOf(phrase);

        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "range": {"startOffset": %d, "endOffset": %d}, \
                "quotedText": "%s", "body": "Active as of when?"}
                """.formatted(start, start + phrase.length(), phrase), reviewerToken());

        assertThat(thread.get("quotedText").asString()).isEqualTo(phrase);
        assertThat(thread.get("range").get("startOffset").asInt()).isEqualTo(start);
        assertThat(thread.get("range").get("endOffset").asInt()).isEqualTo(start + phrase.length());
    }

    @Test
    void aRangeWhoseQuotedTextDoesNotMatchTheHeadIsUnprocessable() throws Exception {
        assertThat(post("/api/reviews/" + reviewId + "/threads", """
                {"sectionKey": "validation-rules-1", "range": {"startOffset": 0, "endOffset": 5}, \
                "quotedText": "wrong text entirely", "body": "?"}
                """, reviewerToken()).statusCode())
                .isEqualTo(422);
    }

    @Test
    void aSectionThatDoesNotExistIsUnprocessable() throws Exception {
        assertThat(post("/api/reviews/" + reviewId + "/threads", """
                {"sectionKey": "no-such-section-1", "body": "?"}
                """, reviewerToken()).statusCode())
                .isEqualTo(422);
    }

    @Test
    void anEditToItsSectionMakesAThreadStaleAndAnUnrelatedSectionDisappearingOrphansAnother() throws Exception {
        String staleThreadId = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "Needs a decision."}
                """, reviewerToken()).get("id").asString();
        String purposeThreadId = openThread(reviewId, """
                {"sectionKey": "purpose-1", "body": "Purpose reads fine."}
                """, reviewerToken()).get("id").asString();

        // Validation Rules is rewritten; Purpose is untouched.
        String editedContent = CLAIMS_V1.replace(
                "The benefit must have remaining balance.", "The benefit must have a positive balance.");
        propose(editedContent, "cafebabe", "octocat");

        assertThat(threadNode(reviewId, staleThreadId).get("anchorState").asString()).isEqualTo("STALE");
        assertThat(threadNode(reviewId, staleThreadId).get("anchorVersionLabel").asString()).isEqualTo("#" + PR_NUMBER);
        assertThat(threadNode(reviewId, purposeThreadId).get("anchorState").asString()).isEqualTo("CURRENT");

        // Now Validation Rules is removed entirely; Purpose and the trailing Notes section are untouched.
        String withoutSection = editedContent.substring(0, editedContent.indexOf("## Validation Rules"))
                + editedContent.substring(editedContent.indexOf("## Notes"));
        propose(withoutSection, "d00dfeed", "octocat");

        assertThat(threadNode(reviewId, staleThreadId).get("anchorState").asString()).isEqualTo("ORPHANED");
        assertThat(threadNode(reviewId, staleThreadId).get("quotedText").asString()).isEqualTo("Validation Rules");
        assertThat(threadNode(reviewId, purposeThreadId).get("anchorState").asString()).isEqualTo("CURRENT");
    }

    @Test
    void theCommentsOwnAuthorCanEditItWithinTheWindowAndNoOneElseCan() throws Exception {
        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "Typo: shuold be active."}
                """, reviewerToken());
        String threadId = thread.get("id").asString();
        String commentId = thread.get("comments").get(0).get("id").asString();

        JsonNode edited = JSON.readTree(put("/api/threads/" + threadId + "/comments/" + commentId, """
                {"body": "Fixed: should be active."}
                """, reviewerToken()).body());
        JsonNode editedComment = edited.get("comments").get(0);
        assertThat(editedComment.get("body").asString()).isEqualTo("Fixed: should be active.");
        assertThat(editedComment.get("editedAt").isNull()).isFalse();

        assertThat(put("/api/threads/" + threadId + "/comments/" + commentId, """
                {"body": "architect tries to edit reviewer's comment"}
                """, architectToken()).statusCode())
                .isEqualTo(409);
    }

    @Test
    void everyCommentResponseCarriesTheContractsRequiredActorKind() throws Exception {
        JsonNode contract = JSON.readTree(get("/api/openapi.json", reviewerToken()).body());
        JsonNode actorRefRequired = contract.get("components").get("schemas").get("ActorRef").get("required");
        assertThat(actorRefRequired.valueStream().map(JsonNode::asString).toList()).contains("actorKind");

        JsonNode thread = openThread(reviewId, """
                {"sectionKey": "validation-rules-1", "body": "provenance check"}
                """, reviewerToken());
        assertThat(thread.get("comments").get(0).get("author").has("actorKind")).isTrue();
    }

    @Test
    void searchingMembersMatchesByHandlePrefix() throws Exception {
        JsonNode members = JSON.readTree(get("/api/members?query=arch", reviewerToken()).body());
        assertThat(members.get("items").valueStream().map(m -> m.get("handle").asString()).toList())
                .contains("architect");
    }

    @Test
    void refusesAnUnauthenticatedCaller() throws Exception {
        assertThat(get("/api/reviews/" + reviewId + "/threads", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/notifications", null).statusCode()).isEqualTo(401);
    }

    private JsonNode openThread(final String forReviewId, final String body, final String bearerToken) throws Exception {
        HttpResponse<String> response = post("/api/reviews/" + forReviewId + "/threads", body, bearerToken);
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body());
    }

    private JsonNode threadNode(final String forReviewId, final String threadId) throws Exception {
        JsonNode list = JSON.readTree(get("/api/reviews/" + forReviewId + "/threads", reviewerToken()).body());
        return list.get("items").valueStream()
                .filter(item -> item.get("id").asString().equals(threadId))
                .findFirst()
                .orElseThrow();
    }

    private int unresolvedBlocking(final String forReviewId) throws Exception {
        return JSON.readTree(get("/api/reviews/" + forReviewId + "/threads", reviewerToken()).body())
                .get("unresolvedBlockingCount").asInt();
    }

    /** Published inside a transaction, because the review capability's own listener runs after that commits. */
    private void propose(final String content, final String headSha, final String author) {
        transactions.executeWithoutResult(status -> events.publishEvent(new SpecChangeProposed(
                CONNECTION, UUID.randomUUID(), REPOSITORY, PR_NUMBER, headSha, author,
                List.of(new ProposedSpec(claimsId, CLAIMS_PATH, content)), Instant.now())));
    }

    private static String reviewerToken() {
        return Keycloak.passwordToken("reviewer", "reviewer");
    }

    private static String architectToken() {
        return Keycloak.passwordToken("architect", "architect");
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

    private HttpResponse<String> put(final String path, final String body, final String bearerToken)
            throws Exception {
        return HTTP.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + localPort + path))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + bearerToken)
                        .timeout(TIMEOUT)
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
