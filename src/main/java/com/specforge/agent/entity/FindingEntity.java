package com.specforge.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * One thing an agent noticed, anchored the way a reviewer's thread is — section key plus an optional
 * range — so a finding and a comment address the same text through the same scheme.
 *
 * <p>The author is a subject id in the same identity table as every human, holding actor kind
 * {@code AGENT}. That is what makes provenance impossible to omit: there is no code path that can
 * store a finding without saying which identity wrote it, and the identity says what it is.
 */
@Entity
@Table(name = "finding")
public class FindingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "check_run_id", nullable = false)
    private UUID checkRunId;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(name = "section_key", nullable = false, length = 128)
    private String sectionKey;

    @Column(name = "range_start")
    private Integer rangeStart;

    @Column(name = "range_end")
    private Integer rangeEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 16)
    private Severity severity;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /** What the agent suggests the section should say; accepting it opens a thread carrying this. */
    @Column(name = "proposed_text", columnDefinition = "TEXT")
    private String proposedText;

    @Column(name = "author_subject_id", nullable = false, length = 64)
    private String authorSubjectId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FindingEntity() {
        // for JPA
    }

    public FindingEntity(
            final UUID id,
            final UUID checkRunId,
            final UUID reviewId,
            final String sectionKey,
            final Integer rangeStart,
            final Integer rangeEnd,
            final Severity severity,
            final String title,
            final String body,
            final String proposedText,
            final String authorSubjectId,
            final Instant now) {
        this.id = id;
        this.checkRunId = checkRunId;
        this.reviewId = reviewId;
        this.sectionKey = sectionKey;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.severity = severity;
        this.title = title;
        this.body = body;
        this.proposedText = proposedText;
        this.authorSubjectId = authorSubjectId;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID checkRunId() {
        return checkRunId;
    }

    public UUID reviewId() {
        return reviewId;
    }

    public String sectionKey() {
        return sectionKey;
    }

    public Integer rangeStart() {
        return rangeStart;
    }

    public Integer rangeEnd() {
        return rangeEnd;
    }

    public Severity severity() {
        return severity;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public String proposedText() {
        return proposedText;
    }

    public String authorSubjectId() {
        return authorSubjectId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
