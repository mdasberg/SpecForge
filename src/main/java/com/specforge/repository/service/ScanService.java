package com.specforge.repository.service;

import com.specforge.platform.api.dto.Scan;
import com.specforge.platform.api.dto.ScanRequest;
import com.specforge.repository.entity.RepositoryScanEntity;
import com.specforge.repository.entity.ScanFileEntity;
import com.specforge.repository.entity.SpecFileFormat;
import com.specforge.repository.exception.Problems;
import com.specforge.repository.repository.ForgeInstallationRepository;
import com.specforge.repository.repository.RepositoryScanRepository;
import com.specforge.repository.repository.ScanFileRepository;
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
public class ScanService {

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
    public Scan start(ScanRequest request) {
        installations
                .findById(request.getInstallationId())
                .orElseThrow(() -> Problems.notFound("No forge installation %s.".formatted(request.getInstallationId())));
        RepositoryScanEntity scan = scans.save(new RepositoryScanEntity(
                UUID.randomUUID(),
                request.getInstallationId(),
                request.getRepositoryFullName(),
                request.getBranch(),
                request.getPathGlob(),
                RepositoryMapper.format(request.getSpecFormat()),
                clock.instant()));
        runner.run(scan.id());
        // Its files are not read back here: the scan has only just been queued, and the wizard
        // polls for the result.
        return RepositoryMapper.scan(scan, List.of());
    }

    @Transactional(readOnly = true)
    public Scan get(UUID scanId) {
        RepositoryScanEntity scan =
                scans.findById(scanId).orElseThrow(() -> Problems.notFound("No scan %s.".formatted(scanId)));
        return RepositoryMapper.scan(scan, scanFiles.findByScanIdOrderByPathAsc(scanId));
    }
}
