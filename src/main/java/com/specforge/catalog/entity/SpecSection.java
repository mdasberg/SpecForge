package com.specforge.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One heading and its extent inside a version. {@code anchorKey} is a heading slug plus an
 * ordinal, and it is the target a discussion or a diff addresses — never a line number, because
 * line numbers move on every edit while a heading slug moves only when the heading itself
 * changes.
 *
 * <p>The line range is stored rather than a copy of the body because the version it points into
 * is immutable, so the offsets cannot drift. A null {@code parentId} is a top-level section.
 */
@Entity
@Table(name = "spec_section")
public class SpecSection {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "version_id", nullable = false)
    private UUID versionId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "anchor_key", nullable = false, length = 512)
    private String anchorKey;

    @Column(name = "heading", nullable = false, length = 512)
    private String heading;

    @Column(name = "level", nullable = false)
    private int level;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "start_line", nullable = false)
    private int startLine;

    @Column(name = "end_line", nullable = false)
    private int endLine;

    protected SpecSection() {
        // for JPA
    }

    public SpecSection(
            UUID id,
            UUID versionId,
            UUID parentId,
            String anchorKey,
            String heading,
            int level,
            int ordinal,
            int startLine,
            int endLine) {
        this.id = id;
        this.versionId = versionId;
        this.parentId = parentId;
        this.anchorKey = anchorKey;
        this.heading = heading;
        this.level = level;
        this.ordinal = ordinal;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public UUID id() {
        return id;
    }

    public UUID versionId() {
        return versionId;
    }

    public UUID parentId() {
        return parentId;
    }

    public String anchorKey() {
        return anchorKey;
    }

    public String heading() {
        return heading;
    }

    public int level() {
        return level;
    }

    public int ordinal() {
        return ordinal;
    }

    public int startLine() {
        return startLine;
    }

    public int endLine() {
        return endLine;
    }
}
