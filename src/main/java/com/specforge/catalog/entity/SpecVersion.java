package com.specforge.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An immutable snapshot of a document's content. Its real identity is {@code contentSha}, the
 * sha256 of the normalised content, which is why re-importing a file whose bytes have not changed
 * creates nothing even when the commit differs.
 *
 * <p>There are deliberately no mutators: a version is never modified once written, and that is
 * what lets a discussion or a diff cite it forever.
 */
@Entity
@Table(name = "spec_version")
public class SpecVersion {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    // Plain UUID, not a @ManyToOne: a version is always loaded through its document's service, so
    // the association would only add a lazy proxy and a second way to write the same row.
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "ordinal", nullable = false)
    private int ordinal;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_sha", nullable = false, length = 64)
    private String contentSha;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "author", length = 255)
    private String author;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SpecVersion() {
        // for JPA
    }

    public SpecVersion(
            UUID id,
            UUID documentId,
            int ordinal,
            String content,
            String contentSha,
            String commitSha,
            String author,
            Instant createdAt) {
        this.id = id;
        this.documentId = documentId;
        this.ordinal = ordinal;
        this.content = content;
        this.contentSha = contentSha;
        this.commitSha = commitSha;
        this.author = author;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID documentId() {
        return documentId;
    }

    public int ordinal() {
        return ordinal;
    }

    public String content() {
        return content;
    }

    public String contentSha() {
        return contentSha;
    }

    public String commitSha() {
        return commitSha;
    }

    public String author() {
        return author;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
