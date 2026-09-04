package com.specforge.review.service;

import com.specforge.catalog.SpecText;
import com.specforge.platform.api.dto.DiffChange;
import com.specforge.platform.api.dto.DiffSection;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The diff, computed once per pair of contents and remembered.
 *
 * <p>The cache is plain SQL rather than an entity because the row is a memo, not a domain object:
 * nothing ever updates one, nothing reads one by anything but its whole key, and the insert wants
 * {@code ON CONFLICT DO NOTHING} so that two reviewers arriving at the same instant simply agree
 * instead of one of them losing a transaction to a primary-key clash.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class DiffService {

    private static final TypeReference<List<DiffSection>> SECTIONS = new TypeReference<>() {};

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final Clock clock;

    /**
     * The section classification and line diff between two contents. Attribution is not part of it:
     * who changed a section is a fact about the version, not about the text, and folding it in would
     * make a cache shared by every review over the same content answer differently per review.
     *
     * <p>It runs in its own transaction because its callers are reads — showing a diff does not
     * change anything a reviewer can see — while remembering the result is a write. Joining the
     * caller's read-only transaction would make the memo fail the moment it was needed, and making
     * the callers writable would give every render permission it has no use for.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<DiffSection> sections(final SpecText base, final SpecText head) {
        final List<DiffSection> cached = jdbc
                .sql("SELECT payload FROM spec_diff WHERE base_sha = :base AND head_sha = :head")
                .param("base", base.contentSha())
                .param("head", head.contentSha())
                .query((rs, row) -> json.readValue(rs.getString(1), SECTIONS))
                .optional()
                .orElse(null);
        if (cached != null) {
            return cached;
        }

        final List<DiffSection> sections = SpecDiffs.compute(base, head);
        jdbc.sql("""
                        INSERT INTO spec_diff (base_sha, head_sha, payload, computed_at)
                        VALUES (:base, :head, :payload, :computedAt)
                        ON CONFLICT (base_sha, head_sha) DO NOTHING""")
                .param("base", base.contentSha())
                .param("head", head.contentSha())
                .param("payload", json.writeValueAsString(sections))
                .param("computedAt", OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC))
                .update();
        return sections;
    }

    /**
     * Stamps each changed section with who changed it and when. The head's commit is the finest
     * attribution SpecForge honestly has: it reads the repository rather than blaming it line by
     * line, so the answer is "this arrived in that commit", not "this word is theirs".
     */
    public List<DiffSection> attributed(
            final List<DiffSection> sections, final String author, final Instant changedAt) {
        for (final DiffSection section : sections) {
            if (section.getChange() == DiffChange.UNCHANGED) {
                continue;
            }
            section.setAuthor(author);
            section.setChangedAt(changedAt == null ? null : changedAt.atOffset(ZoneOffset.UTC));
        }
        return sections;
    }
}
