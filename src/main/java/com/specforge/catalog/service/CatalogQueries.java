package com.specforge.catalog.service;

import com.specforge.platform.api.dto.SpecGrouping;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The SQL the catalogue list is built from. It is assembled rather than written out because the
 * filters are combinable: every one of them is optional, they apply as a conjunction, and the same
 * predicate has to serve the rows, the total and the group counts — three queries that would
 * otherwise drift apart the first time a filter is added to one of them.
 *
 * <p>Nothing from the request is ever concatenated into the SQL. The only text this class chooses
 * is a column name from a fixed table keyed by an enum; every value travels as a bound parameter.
 */
final class CatalogQueries {

    /** The bucket a specification falls in when the grouping dimension is not set on it. */
    static final String UNGROUPED = "Unassigned";

    /**
     * The markers {@code ts_headline} wraps a match in. They are deliberately not HTML: the snippet
     * crosses to a browser, and text the database produced from a document's body is not something
     * a client should be asked to render as markup.
     */
    private static final String HIGHLIGHT = "StartSel=[[, StopSel=]], MaxFragments=1, MaxWords=28, MinWords=10";

    private CatalogQueries() {}

    static String groupExpression(final SpecGrouping grouping) {
        return switch (grouping) {
            case PROJECT -> "d.project";
            case REPOSITORY -> "d.repository_full_name";
            case DOMAIN -> "coalesce(d.domain, :ungrouped)";
            case TEAM -> "coalesce(d.owning_team, :ungrouped)";
        };
    }

    /**
     * The {@code FROM} and {@code WHERE} shared by the three queries. A search term becomes a join
     * rather than another predicate, because the best-matching section is part of the answer and
     * not only part of the question.
     */
    static Where where(final Filters filters) {
        final Map<String, Object> params = new LinkedHashMap<>();
        final StringBuilder from = new StringBuilder("FROM spec_document d");
        if (filters.searching()) {
            from.append("""
                    
                    CROSS JOIN plainto_tsquery('english', :q) AS q(query)
                    JOIN LATERAL (
                        SELECT s.anchor_key, s.heading, ts_rank(s.tsv, q.query) AS rank,
                               ts_headline('english', s.body, q.query, '%s') AS snippet
                        FROM spec_search s
                        WHERE s.document_id = d.id AND s.tsv @@ q.query
                        ORDER BY rank DESC
                        LIMIT 1
                    ) m ON TRUE""".formatted(HIGHLIGHT));
            params.put("q", filters.q());
        }

        final List<String> predicates = new ArrayList<>();
        addIn(predicates, params, "d.status", "statuses", filters.statuses());
        addIn(predicates, params, "d.owner", "owners", filters.owners());
        addIn(predicates, params, "d.owning_team", "teams", filters.teams());
        addIn(predicates, params, "d.domain", "domains", filters.domains());
        if (!filters.tags().isEmpty()) {
            predicates.add("EXISTS (SELECT 1 FROM spec_document_tag t"
                    + " WHERE t.document_id = d.id AND t.tag IN (:tags))");
            params.put("tags", filters.tags());
        }
        final String whereClause = predicates.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", predicates);
        return new Where(from.toString(), whereClause, params);
    }

    private static void addIn(final List<String> predicates, final Map<String, Object> params,
            final String column, final String name, final List<String> values) {
        if (!values.isEmpty()) {
            predicates.add("%s IN (:%s)".formatted(column, name));
            params.put(name, values);
        }
    }

    /** The assembled clauses and the parameters they bind, so a caller cannot bind one without the other. */
    record Where(String from, String whereClause, Map<String, Object> params) {}
}
