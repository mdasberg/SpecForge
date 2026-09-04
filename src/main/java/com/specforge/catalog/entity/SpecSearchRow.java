package com.specforge.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One searchable unit of a specification: a section, or the document itself. The table is
 * denormalised on purpose — search has to rank a heading above a body and return the section that
 * matched, and neither is expressible over the normalised tables without re-parsing content at
 * query time.
 *
 * <p>Only the current version of a document is indexed, and the rows are rebuilt whenever a
 * version is imported, so a search never returns a hit on text that has since been rewritten.
 *
 * <p>The table's {@code tsv} column is deliberately not mapped: PostgreSQL generates it from
 * {@code heading} and {@code body}, and a mapped field would be a second, writable, way to set
 * something the database owns.
 */
@Entity
@Table(name = "spec_search")
public class SpecSearchRow {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    /** The section's anchor, or the empty string for the row carrying the title and path. */
    @Column(name = "anchor_key", nullable = false, length = 512)
    private String anchorKey;

    @Column(name = "heading", nullable = false, length = 512)
    private String heading;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    protected SpecSearchRow() {
        // for JPA
    }

    public SpecSearchRow(
            final UUID id,
            final UUID documentId,
            final UUID versionId,
            final String anchorKey,
            final String heading,
            final String body) {
        this.id = id;
        this.documentId = documentId;
        this.versionId = versionId;
        this.anchorKey = anchorKey;
        this.heading = heading;
        this.body = body;
    }

    public UUID id() {
        return id;
    }

    public UUID documentId() {
        return documentId;
    }

    public UUID versionId() {
        return versionId;
    }

    public String anchorKey() {
        return anchorKey;
    }

    public String heading() {
        return heading;
    }

    public String body() {
        return body;
    }
}
