package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A pull request proposing new specification content. It is not a version: what a pull request
 * says is not yet what the repository says. A further push moves this row's head rather than
 * stacking a second proposal, so a review follows one moving target.
 */
@Entity
@Table(name = "spec_change_proposal")
public class SpecChangeProposalEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "pull_request_number", nullable = false)
    private int pullRequestNumber;

    @Column(name = "head_sha", nullable = false, length = 64)
    private String headSha;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "author", length = 255)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    private ProposalState state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SpecChangeProposalEntity() {
        // for JPA
    }

    public SpecChangeProposalEntity(
            final UUID id, final UUID connectionId, final int pullRequestNumber, final String headSha, final String title, final String author, final Instant now) {
        this.id = id;
        this.connectionId = connectionId;
        this.pullRequestNumber = pullRequestNumber;
        this.headSha = headSha;
        this.title = title;
        this.author = author;
        this.state = ProposalState.OPEN;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateHead(final String headSha, final String title, final String author, final Instant now) {
        this.headSha = headSha;
        this.title = title;
        this.author = author;
        this.updatedAt = now;
    }

    public void close(final ProposalState state, final Instant now) {
        this.state = state;
        this.updatedAt = now;
    }

    public UUID id() {
        return id;
    }

    public UUID connectionId() {
        return connectionId;
    }

    public int pullRequestNumber() {
        return pullRequestNumber;
    }

    public String headSha() {
        return headSha;
    }

    public String title() {
        return title;
    }

    public String author() {
        return author;
    }

    public ProposalState state() {
        return state;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
