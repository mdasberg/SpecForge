package com.specforge.repository.internal;

import com.specforge.catalog.SpecCatalog;
import com.specforge.repository.ReviewOutcome;
import com.specforge.repository.SpecChangeProposed;
import com.specforge.repository.internal.forge.Forge;
import com.specforge.repository.internal.forge.ForgeFile;
import com.specforge.repository.internal.forge.ForgeRef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pull-request synchronisation, which is the default and the reason the product exists: a pull
 * request touching a specification is the moment a team wants review.
 *
 * <p>The head content is recorded as a proposal rather than imported as a version — it is not yet
 * what the repository says. A further push to the same pull request replaces that head instead of
 * stacking a second proposal, so a review follows one moving target.
 */
@Service
class ProposalService {

    private static final Logger log = LoggerFactory.getLogger(ProposalService.class);

    private final SpecChangeProposalRepository proposals;
    private final ProposalFileRepository proposalFiles;
    private final RepositoryConnectionRepository connections;
    private final ForgeInstallationRepository installations;
    private final SpecCatalog catalog;
    private final Forge forge;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ProposalService(
            SpecChangeProposalRepository proposals,
            ProposalFileRepository proposalFiles,
            RepositoryConnectionRepository connections,
            ForgeInstallationRepository installations,
            SpecCatalog catalog,
            Forge forge,
            ApplicationEventPublisher events,
            Clock clock) {
        this.proposals = proposals;
        this.proposalFiles = proposalFiles;
        this.connections = connections;
        this.installations = installations;
        this.catalog = catalog;
        this.forge = forge;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    void pullRequestChanged(
            String repositoryFullName, int number, String baseBranch, String headSha, String title, String author) {
        for (RepositoryConnectionEntity connection :
                connections.findByRepositoryFullNameAndBranch(repositoryFullName, baseBranch)) {
            if (connection.syncMode() != SyncPolicy.ON_PULL_REQUEST || connection.state() == ConnectionState.DEGRADED) {
                continue;
            }
            record(connection, number, headSha, title, author);
        }
    }

    @Transactional
    void pullRequestClosed(String repositoryFullName, int number, boolean merged) {
        Instant now = clock.instant();
        for (RepositoryConnectionEntity connection : connections.findByRepositoryFullName(repositoryFullName)) {
            proposals.findByConnectionIdAndPullRequestNumber(connection.id(), number).ifPresent(proposal -> {
                proposal.close(merged ? ProposalState.MERGED : ProposalState.CLOSED, now);
                proposals.save(proposal);
            });
        }
    }

    private void record(
            RepositoryConnectionEntity connection, int number, String headSha, String title, String author) {
        String installationExternalId = installations
                .findById(connection.installationId())
                .map(ForgeInstallationEntity::externalId)
                .orElse(null);
        if (installationExternalId == null) {
            log.warn("Connection {} has no installation; ignoring pull request {}", connection.id(), number);
            return;
        }
        List<String> changed = SpecPaths.matching(
                forge.changedFiles(installationExternalId, connection.repositoryFullName(), number),
                connection.pathGlob());
        if (changed.isEmpty()) {
            return;
        }

        Instant now = clock.instant();
        SpecChangeProposalEntity proposal = proposals
                .findByConnectionIdAndPullRequestNumber(connection.id(), number)
                .orElseGet(() -> proposals.save(new SpecChangeProposalEntity(
                        UUID.randomUUID(), connection.id(), number, headSha, title, author, now)));
        proposal.updateHead(headSha, title, author, now);
        proposals.save(proposal);

        // The head moved, so the previous head's files are no longer what is proposed. The flush
        // is load-bearing: JPA orders inserts before deletes within one flush, so without it the
        // new rows hit the (proposal, path) unique constraint against rows this delete is about to
        // remove.
        proposalFiles.deleteByProposalId(proposal.id());
        proposalFiles.flush();
        ForgeRef ref = new ForgeRef(connection.repositoryFullName(), headSha);
        List<ProposalFileEntity> files = new ArrayList<>(changed.size());
        List<UUID> documentIds = new ArrayList<>();
        for (String path : changed) {
            Optional<ForgeFile> file = forge.readFile(installationExternalId, ref, path);
            if (file.isEmpty()) {
                continue;
            }
            files.add(new ProposalFileEntity(
                    UUID.randomUUID(), proposal.id(), path, sha256(file.get().content()), file.get().content()));
            catalog.find(connection.id(), path).ifPresent(documentId -> {
                catalog.proposeChange(documentId);
                documentIds.add(documentId);
            });
        }
        proposalFiles.saveAll(files);

        events.publishEvent(new SpecChangeProposed(
                connection.id(),
                proposal.id(),
                connection.repositoryFullName(),
                number,
                headSha,
                List.copyOf(documentIds),
                now));

        // Pending the moment the change is proposed: the pull request says a review is running
        // before anyone has looked, rather than looking green until someone remembers to report.
        forge.reportReviewStatus(
                installationExternalId,
                connection.repositoryFullName(),
                headSha,
                com.specforge.repository.internal.forge.ReviewStatusState.PENDING,
                "SpecForge review in progress",
                null);
    }

    @Transactional
    void report(UUID proposalId, ReviewOutcome outcome, String description) {
        SpecChangeProposalEntity proposal = proposals
                .findById(proposalId)
                .orElseThrow(() -> Problems.notFound("No proposed change %s.".formatted(proposalId)));
        RepositoryConnectionEntity connection = connections
                .findById(proposal.connectionId())
                .orElseThrow(() -> Problems.notFound("No connection behind proposal %s.".formatted(proposalId)));
        String installationExternalId = installations
                .findById(connection.installationId())
                .map(ForgeInstallationEntity::externalId)
                .orElseThrow(() -> Problems.notFound("No installation behind proposal %s.".formatted(proposalId)));
        forge.reportReviewStatus(
                installationExternalId,
                connection.repositoryFullName(),
                proposal.headSha(),
                switch (outcome) {
                    case PENDING -> com.specforge.repository.internal.forge.ReviewStatusState.PENDING;
                    case APPROVED -> com.specforge.repository.internal.forge.ReviewStatusState.SUCCESS;
                    case CHANGES_REQUESTED -> com.specforge.repository.internal.forge.ReviewStatusState.FAILURE;
                },
                description,
                null);
    }

    private static String sha256(String content) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
