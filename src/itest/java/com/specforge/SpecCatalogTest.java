package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The catalogue as a client meets it: grouped, filtered, searched and read. The documents are
 * seeded through the module's own import, not through inserts, because the search index is built
 * by importing and a fixture that wrote the rows itself would prove nothing about it.
 */
class SpecCatalogTest extends BaseIntegrationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID CARE_CONNECTION = UUID.randomUUID();
    private static final UUID BILLING_CONNECTION = UUID.randomUUID();

    private static final String BILLING = """
            # Billing

            ## Validation Rules

            A policy must exist before an invoice is raised.
            """;
    private static final String CLAIMS = """
            # Claims

            ## Purpose

            Claims are settled after validation of the member's cover.
            """;
    private static final String LEDGER = """
            # Ledger

            ## Purpose

            Every movement is double entry.
            """;

    @Autowired
    private SpecCatalog catalog;

    @Autowired
    private JdbcClient jdbc;

    private UUID billingId;
    private UUID claimsId;

    @BeforeEach
    void seed() {
        jdbc.sql("TRUNCATE TABLE spec_search, spec_section, spec_version, spec_document_tag, spec_document CASCADE")
                .update();
        billingId = importSpec(CARE_CONNECTION, "acme/care-specs", "openspec/specs/billing/spec.md",
                "Care Management", "billing", "Payments", "ada", Set.of("money"), BILLING);
        claimsId = importSpec(CARE_CONNECTION, "acme/care-specs", "openspec/specs/claims/spec.md",
                "Care Management", "claims", "Payments", "linus", Set.of("claims"), CLAIMS);
        importSpec(BILLING_CONNECTION, "acme/billing-specs", "openspec/specs/ledger/spec.md",
                "Billing", "ledger", "Finance", "ada", Set.of("money"), LEDGER);
        catalog.proposeChange(claimsId);
    }

    @Test
    void groupsByProjectWithCountsThatSumToTheTotal() throws Exception {
        JsonNode list = specs("");

        assertThat(list.get("groupBy").asString()).isEqualTo("PROJECT");
        assertThat(list.get("total").asInt()).isEqualTo(3);
        assertThat(groupCounts(list)).containsExactly("Billing=1", "Care Management=2");
        assertThat(list.get("items").size()).isEqualTo(3);
    }

    @Test
    void regroupingKeepsTheSameSpecificationsAndStillSumsToTheTotal() throws Exception {
        JsonNode byDomain = specs("?groupBy=DOMAIN");

        assertThat(byDomain.get("total").asInt()).isEqualTo(3);
        assertThat(groupCounts(byDomain)).containsExactly("billing=1", "claims=1", "ledger=1");
        assertThat(ids(byDomain)).containsExactlyInAnyOrderElementsOf(ids(specs("")));
    }

    @Test
    void groupsByRepositoryFromTheDocumentItself() throws Exception {
        assertThat(groupCounts(specs("?groupBy=REPOSITORY")))
                .containsExactly("acme/billing-specs=1", "acme/care-specs=2");
    }

    @Test
    void filtersCombineAsAConjunction() throws Exception {
        JsonNode both = specs("?status=IN_REVIEW&domain=claims");

        assertThat(both.get("total").asInt()).isEqualTo(1);
        assertThat(both.get("items").get(0).get("id").asString()).isEqualTo(claimsId.toString());
        // Each half on its own matches more, which is what makes the conjunction visible.
        assertThat(specs("?status=IN_REVIEW").get("total").asInt()).isEqualTo(1);
        assertThat(specs("?domain=billing&domain=claims").get("total").asInt()).isEqualTo(2);
    }

    @Test
    void reportsAnEmptyResultForACombinationThatMatchesNothing() throws Exception {
        JsonNode none = specs("?status=APPROVED&domain=billing");

        assertThat(none.get("total").asInt()).isZero();
        assertThat(none.get("items")).isEmpty();
        assertThat(none.get("groups")).isEmpty();
    }

    @Test
    void filtersByTag() throws Exception {
        assertThat(specs("?tag=money").get("total").asInt()).isEqualTo(2);
        assertThat(specs("?tag=claims").get("total").asInt()).isEqualTo(1);
    }

    @Test
    void searchRanksAHeadingMatchAboveABodyMatch() throws Exception {
        JsonNode found = specs("?q=validation");

        assertThat(found.get("total").asInt()).isEqualTo(2);
        assertThat(ids(found)).containsExactly(billingId.toString(), claimsId.toString());
        assertThat(found.get("items").get(0).get("match").get("heading").asString())
                .isEqualTo("Validation Rules");
        // The lower-ranked hit matched in a body, so its snippet is the surrounding text with the
        // term marked. The heading hit has nothing to mark in its body, which is why it is not
        // asserted here.
        assertThat(found.get("items").get(1).get("match").get("snippet").asString())
                .contains("[[validation]]");
    }

    @Test
    void searchMatchesTheTitleAndThePathThroughTheSameIndex() throws Exception {
        assertThat(ids(specs("?q=ledger"))).hasSize(1);
    }

    @Test
    void searchAppliesOnTopOfTheActiveFiltersRatherThanReplacingThem() throws Exception {
        assertThat(specs("?q=validation").get("total").asInt()).isEqualTo(2);
        assertThat(ids(specs("?q=validation&status=IN_REVIEW"))).containsExactly(claimsId.toString());
    }

    @Test
    void pagesThroughTheResultWithACursor() throws Exception {
        JsonNode first = specs("?limit=2");

        assertThat(first.get("items").size()).isEqualTo(2);
        assertThat(first.get("total").asInt()).isEqualTo(3);
        assertThat(first.get("cursor").asString()).isEqualTo("2");

        JsonNode second = specs("?limit=2&cursor=" + first.get("cursor").asString());
        assertThat(second.get("items").size()).isEqualTo(1);
        assertThat(second.get("cursor").isNull()).isTrue();
        assertThat(ids(second)).doesNotContainAnyElementsOf(ids(first));
    }

    @Test
    void rejectsACursorThatDidNotComeFromAResponse() throws Exception {
        assertThat(get("/api/specs?cursor=tomorrow", token()).statusCode()).isEqualTo(400);
    }

    @Test
    void rendersTheCurrentVersionWithItsOutlineAndItsVersionSelector() throws Exception {
        JsonNode detail = JSON.readTree(get("/api/specs/" + billingId, token()).body());

        assertThat(detail.get("title").asString()).isEqualTo("Billing");
        assertThat(detail.get("repositoryFullName").asString()).isEqualTo("acme/care-specs");
        assertThat(detail.get("version").get("ordinal").asInt()).isEqualTo(1);
        assertThat(detail.get("version").get("current").asBoolean()).isTrue();
        assertThat(detail.get("version").get("content").asString()).contains("A policy must exist");
        assertThat(outline(detail.get("version"))).containsExactly("billing-1", "validation-rules-1");
        assertThat(detail.get("versions").size()).isEqualTo(1);
    }

    @Test
    void rendersAnyHistoricalVersionAndSaysWhichOneIsCurrent() throws Exception {
        importSpec(CARE_CONNECTION, "acme/care-specs", "openspec/specs/billing/spec.md",
                "Care Management", "billing", "Payments", "ada", Set.of("money"),
                BILLING + "\n## Preconditions\n\nThe member is covered.\n");

        JsonNode older = JSON.readTree(get("/api/specs/" + billingId + "?version=1", token()).body());

        assertThat(older.get("version").get("ordinal").asInt()).isEqualTo(1);
        assertThat(older.get("version").get("current").asBoolean()).isFalse();
        assertThat(older.get("version").get("content").asString()).doesNotContain("The member is covered");
        assertThat(older.get("versions").size()).isEqualTo(2);
        assertThat(older.get("versions").get(1).get("current").asBoolean()).isTrue();
        assertThat(get("/api/specs/" + billingId + "?version=9", token()).statusCode()).isEqualTo(404);
    }

    @Test
    void searchFollowsTheCurrentVersionRatherThanTheOneItIndexedFirst() throws Exception {
        assertThat(specs("?q=escrow").get("total").asInt()).isZero();

        importSpec(CARE_CONNECTION, "acme/care-specs", "openspec/specs/billing/spec.md",
                "Care Management", "billing", "Payments", "ada", Set.of("money"),
                "# Billing\n\n## Preconditions\n\nThe premium is held in escrow.\n");

        assertThat(ids(specs("?q=escrow"))).containsExactly(billingId.toString());
        // The text the previous version carried is gone from the index with it.
        assertThat(specs("?q=invoice").get("total").asInt()).isZero();
    }

    @Test
    void showsEachProjectsRepositoriesCountsAndOpenReviews() throws Exception {
        JsonNode projects = JSON.readTree(get("/api/projects", token()).body()).get("items");

        assertThat(projects.size()).isEqualTo(2);
        assertThat(projects.get(0).get("name").asString()).isEqualTo("Billing");
        JsonNode care = projects.get(1);
        assertThat(care.get("name").asString()).isEqualTo("Care Management");
        assertThat(care.get("team").asString()).isEqualTo("Payments");
        assertThat(care.get("specCount").asInt()).isEqualTo(2);
        assertThat(care.get("openReviews").asInt()).isEqualTo(1);
        assertThat(care.get("counts").get("draft").asInt()).isEqualTo(1);
        assertThat(care.get("counts").get("inReview").asInt()).isEqualTo(1);
        assertThat(repositories(care)).containsExactly("acme/care-specs");
    }

    @Test
    void refusesAnUnauthenticatedCaller() throws Exception {
        assertThat(get("/api/specs", null).statusCode()).isEqualTo(401);
        assertThat(get("/api/projects", null).statusCode()).isEqualTo(401);
    }

    private UUID importSpec(final UUID connectionId, final String repository, final String path,
            final String project, final String domain, final String team, final String owner,
            final Set<String> tags, final String content) {
        return catalog
                .importVersion(new SpecImport(connectionId, repository, path, project, domain, team, owner,
                        tags, content, "c0ffee"))
                .documentId();
    }

    private JsonNode specs(final String query) throws Exception {
        return JSON.readTree(get("/api/specs" + query, token()).body());
    }

    private static String token() {
        return Keycloak.passwordToken("reviewer", "reviewer");
    }

    private static List<String> ids(final JsonNode list) {
        return list.get("items").valueStream().map(item -> item.get("id").asString()).toList();
    }

    private static List<String> groupCounts(final JsonNode list) {
        return list.get("groups").valueStream()
                .map(group -> group.get("key").asString() + "=" + group.get("count").asInt())
                .toList();
    }

    private static List<String> outline(final JsonNode version) {
        return version.get("sections").valueStream().map(section -> section.get("anchorKey").asString()).toList();
    }

    private static List<String> repositories(final JsonNode project) {
        return project.get("repositories").valueStream().map(JsonNode::asString).toList();
    }
}
