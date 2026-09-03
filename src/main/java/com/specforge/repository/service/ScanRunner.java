package com.specforge.repository.service;

import com.specforge.repository.entity.Classification;
import com.specforge.repository.entity.RepositoryScanEntity;
import com.specforge.repository.entity.ScanFileEntity;
import com.specforge.repository.exception.ForgeException;
import com.specforge.repository.forge.Forge;
import com.specforge.repository.forge.ForgeFile;
import com.specforge.repository.forge.ForgeRef;
import com.specforge.repository.repository.ForgeInstallationRepository;
import com.specforge.repository.repository.RepositoryScanRepository;
import com.specforge.repository.repository.ScanFileRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.specforge.repository.event.ScanRequested;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

/**
 * Runs a scan off the request thread. A repository with hundreds of matched files takes longer
 * than a wizard step should block for, so the scan reports progress through its own status instead
 * of holding the connection open.
 */
@RequiredArgsConstructor
@Component
public class ScanRunner {

    private static final Logger log = LoggerFactory.getLogger(ScanRunner.class);

    private final RepositoryScanRepository scans;
    private final ScanFileRepository scanFiles;
    private final ForgeInstallationRepository installations;
    private final Forge forge;
    private final Clock clock;

    /**
     * Runs once the transaction that recorded the scan has committed, on another thread. Without
     * the commit boundary the runner would race the writer and find no row at all.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void run(final ScanRequested requested) {
        final UUID scanId = requested.scanId();
        final RepositoryScanEntity scan = scans.findById(scanId).orElse(null);
        if (scan == null) {
            log.warn("Scan {} vanished before it ran", scanId);
            return;
        }
        scan.start();
        scans.save(scan);
        try {
            final String installationExternalId = installations
                    .findById(scan.installationId())
                    .orElseThrow(() -> new ForgeException("The installation behind this scan no longer exists."))
                    .externalId();
            final ForgeRef ref = new ForgeRef(scan.repositoryFullName(), scan.branch());
            final List<String> matched = SpecPaths.matching(forge.listFiles(installationExternalId, ref),
                    scan.pathGlob());

            final List<ScanFileEntity> rows = new ArrayList<>(matched.size());
            int importable = 0;
            int proposals = 0;
            int unparsable = 0;
            for (final String path : matched) {
                final Optional<ForgeFile> file = forge.readFile(installationExternalId, ref, path);
                final SpecClassifier.Verdict verdict = file
                        .map(found -> SpecClassifier.classify(path, found.content()))
                        .orElseGet(() -> new SpecClassifier.Verdict(
                                Classification.UNPARSABLE, "The file could not be read from the repository."));
                switch (verdict.classification()) {
                    case IMPORTABLE_SPEC -> importable++;
                    case CHANGE_PROPOSAL -> proposals++;
                    case UNPARSABLE -> unparsable++;
                }
                rows.add(new ScanFileEntity(
                        UUID.randomUUID(), scan.id(), path, verdict.classification(), verdict.reason()));
            }
            scanFiles.saveAll(rows);
            scan.succeed(importable, proposals, unparsable, clock.instant());
            scans.save(scan);
        } catch (final RuntimeException e) {
            log.warn("Scan {} failed", scanId, e);
            scan.fail(e.getMessage(), clock.instant());
            scans.save(scan);
        }
    }
}
