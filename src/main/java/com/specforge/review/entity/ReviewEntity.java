package com.specforge.review.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A review of one specification between two versions.
 *
 * <p>The head is held as content rather than as a version id because content proposed by a pull
 * request is not a version: the repository has not accepted it, and inventing a version for it would
 * make the catalogue claim something the source of truth never said. The base is held as an ordinal
 * and resolved through the catalogue, since that end always is an imported version.
 *
 * <p>A further push to the same pull request replaces the head in place. A second review would leave
 * two answers to "is this specification approved", and the product only has room for one.
 */
@Entity
@Table(name = "review")
public class ReviewEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    // Plain UUID rather than an association: the specification belongs to the catalog module, and a
    // mapping here would be a second way to write rows this module does not own.
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "base_version_ordinal", nullable = false)
    private int baseVersionOrdinal;

    // Denormalised from the version: listing reviews would otherwise read the catalogue once per
    // row, and this is also half the key the computed diff is cached under.
    @Column(name = "base_content_sha", nullable = false, length = 64)
    private String baseContentSha;

    /** Set only when the head is an imported version; a pull request head has no ordinal. */
    @Column(name = "head_version_ordinal")
    private Integer headVersionOrdinal;

    @Column(name = "head_content", nullable = false, columnDefinition = "TEXT")
    private String headContent;

    @Column(name = "head_content_sha", nullable = false, length = 64)
    private String headContentSha;

    @Column(name = "head_author", length = 255)
    private String headAuthor;

    @Column(name = "head_commit_sha", length = 64)
    private String headCommitSha;

    @Column(name = "head_created_at")
    private Instant headCreatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private ReviewState state;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(name = "repository_full_name", length = 512)
    private String repositoryFullName;

    @Column(name = "pull_request_number")
    private Integer pullRequestNumber;

    @Column(name = "opened_by", length = 255)
    private String openedBy;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected ReviewEntity() {
        // for JPA
    }

    public ReviewEntity(
            final UUID id,
            final UUID documentId,
            final int baseVersionOrdinal,
            final String baseContentSha,
            final String openedBy,
            final Instant openedAt) {
        this.id = id;
        this.documentId = documentId;
        this.baseVersionOrdinal = baseVersionOrdinal;
        this.baseContentSha = baseContentSha;
        this.state = ReviewState.OPEN;
        this.openedBy = openedBy;
        this.openedAt = openedAt;
        this.updatedAt = openedAt;
    }

    /** Points the review at a pull request, so an approval knows where to report back to. */
    public void fromPullRequest(final UUID proposalId, final String repositoryFullName, final int number) {
        this.proposalId = proposalId;
        this.repositoryFullName = repositoryFullName;
        this.pullRequestNumber = number;
    }

    /**
     * Replaces the head. Returns false when the content is byte-for-byte what the review already
     * had, which is what makes a re-delivered webhook a no-op rather than a second carry of every
     * anchor over content that never moved.
     */
    public boolean advanceHead(
            final String content,
            final String contentSha,
            final Integer versionOrdinal,
            final String author,
            final String commitSha,
            final Instant createdAt,
            final Instant now) {
        if (contentSha.equals(this.headContentSha)) {
            return false;
        }
        this.headContent = content;
        this.headContentSha = contentSha;
        this.headVersionOrdinal = versionOrdinal;
        this.headAuthor = author;
        this.headCommitSha = commitSha;
        this.headCreatedAt = createdAt;
        this.updatedAt = now;
        return true;
    }

    public void close(final Instant now) {
        this.state = ReviewState.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID documentId() {
        return documentId;
    }

    public int baseVersionOrdinal() {
        return baseVersionOrdinal;
    }

    public String baseContentSha() {
        return baseContentSha;
    }

    public Integer headVersionOrdinal() {
        return headVersionOrdinal;
    }

    public String headContent() {
        return headContent;
    }

    public String headContentSha() {
        return headContentSha;
    }

    public String headAuthor() {
        return headAuthor;
    }

    public String headCommitSha() {
        return headCommitSha;
    }

    public Instant headCreatedAt() {
        return headCreatedAt;
    }

    public ReviewState state() {
        return state;
    }

    public UUID proposalId() {
        return proposalId;
    }

    public String repositoryFullName() {
        return repositoryFullName;
    }

    public Integer pullRequestNumber() {
        return pullRequestNumber;
    }

    public String openedBy() {
        return openedBy;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }
}
