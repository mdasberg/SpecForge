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
import com.specforge.repository.event.ScanRequested;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The wizard's second step. A scan is stored rather than recomputed so stepping back and forth in
 * the wizard re-reads a result instead of hitting the forge again, and so the connection that
 * follows can be checked against what the administrator actually saw.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class ScanService {

    private final RepositoryScanRepository scans;
    private final ScanFileRepository scanFiles;
    private final ForgeInstallationRepository installations;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public Scan start(final ScanRequest request) {
        installations
                .findById(request.getInstallationId())
                .orElseThrow(() -> Problems.notFound("No forge installation %s.".formatted(request.getInstallationId())));
        final RepositoryScanEntity scan = scans.save(new RepositoryScanEntity(
                UUID.randomUUID(),
                request.getInstallationId(),
                request.getRepositoryFullName(),
                request.getBranch(),
                request.getPathGlob(),
                RepositoryMapper.format(request.getSpecFormat()),
                clock.instant()));
        events.publishEvent(new ScanRequested(scan.id()));
        // Its files are not read back here: the scan has only just been queued, and the wizard
        // polls for the result.
        return RepositoryMapper.scan(scan, List.of());
    }

    @Transactional(readOnly = true)
    public Scan get(final UUID scanId) {
        final RepositoryScanEntity scan =
                scans.findById(scanId).orElseThrow(() -> Problems.notFound("No scan %s.".formatted(scanId)));
        return RepositoryMapper.scan(scan, scanFiles.findByScanIdOrderByPathAsc(scanId));
    }
}
