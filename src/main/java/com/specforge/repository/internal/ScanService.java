package com.specforge.repository.internal;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The wizard's second step. A scan is stored rather than recomputed so stepping back and forth in
 * the wizard re-reads a result instead of hitting the forge again, and so the connection that
 * follows can be checked against what the administrator actually saw.
 */
@Service
class ScanService {

    private final RepositoryScanRepository scans;
    private final ScanFileRepository scanFiles;
    private final ForgeInstallationRepository installations;
    private final ScanRunner runner;
    private final Clock clock;

    ScanService(
            RepositoryScanRepository scans,
            ScanFileRepository scanFiles,
            ForgeInstallationRepository installations,
            ScanRunner runner,
            Clock clock) {
        this.scans = scans;
        this.scanFiles = scanFiles;
        this.installations = installations;
        this.runner = runner;
        this.clock = clock;
    }

    /**
     * Not transactional on purpose: the row has to be committed before the asynchronous run can
     * load it, and a scan carries no state that a rollback would need to undo.
     */
    RepositoryScanEntity start(
            UUID installationId, String repositoryFullName, String branch, String pathGlob, SpecFileFormat format) {
        installations
                .findById(installationId)
                .orElseThrow(() -> Problems.notFound("No forge installation %s.".formatted(installationId)));
        RepositoryScanEntity scan = scans.save(new RepositoryScanEntity(
                UUID.randomUUID(), installationId, repositoryFullName, branch, pathGlob, format, clock.instant()));
        runner.run(scan.id());
        return scan;
    }

    @Transactional(readOnly = true)
    RepositoryScanEntity get(UUID scanId) {
        return scans.findById(scanId).orElseThrow(() -> Problems.notFound("No scan %s.".formatted(scanId)));
    }

    @Transactional(readOnly = true)
    List<ScanFileEntity> files(UUID scanId) {
        return scanFiles.findByScanIdOrderByPathAsc(scanId);
    }
}
