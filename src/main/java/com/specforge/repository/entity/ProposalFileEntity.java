package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One file's content as a pull request proposes it. The content is stored rather than fetched on
 * demand: a review has to keep citing what was proposed even after the branch moves or is deleted.
 */
@Entity
@Table(name = "spec_change_proposal_file")
public class ProposalFileEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "path", nullable = false, length = 1024)
    private String path;

    @Column(name = "content_sha", nullable = false, length = 64)
    private String contentSha;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    protected ProposalFileEntity() {
        // for JPA
    }

    public ProposalFileEntity(final UUID id, final UUID proposalId, final String path, final String contentSha,
            final String content) {
        this.id = id;
        this.proposalId = proposalId;
        this.path = path;
        this.contentSha = contentSha;
        this.content = content;
    }

    public UUID id() {
        return id;
    }

    public UUID proposalId() {
        return proposalId;
    }

    public String path() {
        return path;
    }

    public String contentSha() {
        return contentSha;
    }

    public String content() {
        return content;
    }
}
