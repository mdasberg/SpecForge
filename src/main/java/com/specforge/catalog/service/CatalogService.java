package com.specforge.catalog.service;

import com.specforge.catalog.repository.SpecDocumentRepository;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.ProjectList;
import com.specforge.platform.api.dto.ProjectOverview;
import com.specforge.platform.api.dto.SpecDetail;
import com.specforge.platform.api.dto.SpecGroup;
import com.specforge.platform.api.dto.SpecGrouping;
import com.specforge.platform.api.dto.SpecList;
import com.specforge.platform.api.dto.SpecMatch;
import com.specforge.platform.api.dto.SpecStatus;
import com.specforge.platform.api.dto.SpecStatusCounts;
import com.specforge.platform.api.dto.SpecSummary;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The catalogue's read side: browse, group, filter and search. It is SQL rather than JPA because
 * the three things this has to do — combine optional filters, count every group over the whole
 * filtered result, and rank a full-text match while naming the section that matched — are all
 * things the database does in one pass and an entity graph does in several.
 *
 * <p>The list envelope mirrors {@link com.specforge.platform.api.Page}: items, a total and a
 * cursor. It is a generated contract type rather than that record because the API's types come
 * from the contract, and a generic cannot be expressed there.
 */
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CatalogService {

    /** Matches the contract's own default, so an absent `limit` and an explicit one agree. */
    private static final int DEFAULT_LIMIT = 50;

    private final JdbcClient jdbc;
    private final SpecDocumentRepository documents;
    private final SpecVersionReader versions;

    public SpecList list(final SpecGrouping requestedGrouping, final List<SpecStatus> statuses,
            final List<String> owners, final List<String> teams, final List<String> domains,
            final List<String> tags, final String q, final Integer limit, final String cursor) {
        final SpecGrouping grouping = requestedGrouping == null ? SpecGrouping.PROJECT : requestedGrouping;
        final Filters filters = Filters.of(statuses, owners, teams, domains, tags, q);
        final CatalogQueries.Where where = CatalogQueries.where(filters);
        final String groupExpression = CatalogQueries.groupExpression(grouping);
        final int pageSize = limit == null ? DEFAULT_LIMIT : limit;
        final int offset = offsetOf(cursor);

        final Map<String, Object> params = new LinkedHashMap<>(where.params());
        params.put("ungrouped", CatalogQueries.UNGROUPED);

        final long total = jdbc.sql("SELECT count(*) %s %s".formatted(where.from(), where.whereClause()))
                .params(params)
                .query(Long.class)
                .single();

        final List<SpecGroup> groups = jdbc
                .sql("""
                        SELECT %s AS group_key, count(*) AS group_count
                        %s
                        %s
                        GROUP BY group_key
                        ORDER BY group_key"""
                        .formatted(groupExpression, where.from(), where.whereClause()))
                .params(params)
                .query((rs, row) -> new SpecGroup(rs.getString("group_key"), rs.getLong("group_count")))
                .list();

        final Map<String, Object> pageParams = new LinkedHashMap<>(params);
        // One row more than asked for: whether a next page exists is then a fact about the result
        // rather than a second count that can disagree with it.
        pageParams.put("limit", pageSize + 1);
        pageParams.put("offset", offset);
        final List<SpecSummary> rows = jdbc
                .sql("""
                        SELECT d.id, d.title, d.path, d.project, d.repository_full_name, d.domain,
                               d.owning_team, d.owner, d.status, d.updated_at,
                               %s AS group_key,
                               coalesce((SELECT max(v.ordinal) FROM spec_version v
                                         WHERE v.document_id = d.id), 0) AS current_version%s
                        %s
                        %s
                        ORDER BY %sd.updated_at DESC, d.title ASC
                        LIMIT :limit OFFSET :offset"""
                        .formatted(
                                groupExpression,
                                filters.searching() ? ",\n       m.anchor_key, m.heading, m.snippet" : "",
                                where.from(),
                                where.whereClause(),
                                filters.searching() ? "m.rank DESC, " : ""))
                .params(pageParams)
                .query((rs, row) -> {
                    final SpecSummary summary = new SpecSummary(
                            rs.getObject("id", UUID.class),
                            rs.getString("title"),
                            rs.getString("path"),
                            rs.getString("project"),
                            rs.getString("repository_full_name"),
                            SpecStatus.fromValue(rs.getString("status")),
                            new ArrayList<>(),
                            rs.getInt("current_version"),
                            rs.getObject("updated_at", OffsetDateTime.class)
                                    .withOffsetSameInstant(ZoneOffset.UTC),
                            rs.getString("group_key"));
                    summary.setDomain(rs.getString("domain"));
                    summary.setOwningTeam(rs.getString("owning_team"));
                    summary.setOwner(rs.getString("owner"));
                    if (filters.searching()) {
                        summary.setMatch(new SpecMatch(
                                rs.getString("anchor_key"), rs.getString("heading"), rs.getString("snippet")));
                    }
                    return summary;
                })
                .list();

        final boolean hasMore = rows.size() > pageSize;
        final List<SpecSummary> items = hasMore ? rows.subList(0, pageSize) : rows;
        attachTags(items);
        return new SpecList(items, groups, grouping, total)
                .cursor(hasMore ? String.valueOf(offset + pageSize) : null);
    }

    public SpecDetail get(final UUID specId, final Integer version) {
        return documents
                .findById(specId)
                .map(document -> versions.detail(document, version))
                .orElseThrow(() -> Problems.notFound("No specification %s.".formatted(specId)));
    }

    /**
     * Built from the imported specifications rather than from the connection records, so the
     * overview says what the catalogue actually holds. Open reviews are the specifications
     * currently in review: until the review capability exists, that count is the honest answer
     * rather than a zero standing in for one.
     */
    public ProjectList projects() {
        final Map<String, List<String>> repositories = new HashMap<>();
        jdbc.sql("SELECT DISTINCT project, repository_full_name FROM spec_document ORDER BY 1, 2")
                .query((rs, row) -> repositories
                        .computeIfAbsent(rs.getString(1), project -> new ArrayList<>())
                        .add(rs.getString(2)))
                .list();

        final List<ProjectOverview> projects = jdbc
                .sql("""
                        SELECT d.project,
                               min(d.owning_team) AS team,
                               count(*) AS spec_count,
                               count(*) FILTER (WHERE d.status = 'DRAFT') AS draft,
                               count(*) FILTER (WHERE d.status = 'IN_REVIEW') AS in_review,
                               count(*) FILTER (WHERE d.status = 'CHANGES_REQUESTED') AS changes_requested,
                               count(*) FILTER (WHERE d.status = 'APPROVED') AS approved,
                               count(*) FILTER (WHERE d.status = 'IMPLEMENTED') AS implemented
                        FROM spec_document d
                        GROUP BY d.project
                        ORDER BY d.project""")
                .query((rs, row) -> {
                    final String name = rs.getString("project");
                    final SpecStatusCounts counts = new SpecStatusCounts(
                            rs.getLong("draft"),
                            rs.getLong("in_review"),
                            rs.getLong("changes_requested"),
                            rs.getLong("approved"),
                            rs.getLong("implemented"));
                    final ProjectOverview project = new ProjectOverview(
                            name,
                            repositories.getOrDefault(name, List.of()),
                            rs.getLong("spec_count"),
                            counts,
                            counts.getInReview());
                    project.setTeam(rs.getString("team"));
                    return project;
                })
                .list();
        return new ProjectList(projects);
    }

    /** One query for the whole page's tags rather than one per row. */
    private void attachTags(final List<SpecSummary> items) {
        if (items.isEmpty()) {
            return;
        }
        final Map<UUID, SpecSummary> byId = new LinkedHashMap<>();
        items.forEach(item -> byId.put(item.getId(), item));
        jdbc.sql("SELECT document_id, tag FROM spec_document_tag WHERE document_id IN (:ids) ORDER BY tag")
                .param("ids", byId.keySet())
                .query((rs, row) -> byId.get(rs.getObject(1, UUID.class)).getTags().add(rs.getString(2)))
                .list();
    }

    /**
     * The cursor is the offset the next page starts at. It is checked rather than trusted: it
     * arrives in a URL, and a URL is edited.
     */
    private static int offsetOf(final String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            final int offset = Integer.parseInt(cursor.strip());
            if (offset < 0) {
                throw new NumberFormatException(cursor);
            }
            return offset;
        } catch (final NumberFormatException e) {
            throw Problems.badRequest("'%s' is not a cursor from a previous response.".formatted(cursor));
        }
    }

}
