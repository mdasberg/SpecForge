package com.specforge.repository.api;

import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.Connection;
import com.specforge.platform.api.dto.ConnectionList;
import com.specforge.platform.api.dto.ConnectionRequest;
import com.specforge.platform.api.dto.ForgeInstallationList;
import com.specforge.platform.api.dto.ImportRun;
import com.specforge.platform.api.dto.ImportRunList;
import com.specforge.platform.api.dto.Scan;
import com.specforge.platform.api.dto.ScanRequest;
import com.specforge.platform.api.dto.SpecContentUpdate;
import com.specforge.platform.api.generated.RepositoryApi;
import com.specforge.repository.service.ConnectionService;
import com.specforge.repository.service.ImportService;
import com.specforge.repository.service.InstallationService;
import com.specforge.repository.service.ScanService;
import com.specforge.repository.service.SpecContentService;
import com.specforge.repository.service.WebhookService;
import com.specforge.repository.service.WebhookVerifier;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The repository capability's HTTP surface. It implements the interface generated from
 * {@code specforge-api.yaml}, so a change to the contract that this does not follow fails to
 * compile rather than reaching a client.
 *
 * <p>There is no mapping here on purpose: the services speak the contract's own types, and this
 * class only routes and authorises.
 */
@RequiredArgsConstructor
@RestController
class RepositoryController implements RepositoryApi {

    private final ConnectionService connections;
    private final ScanService scans;
    private final ImportService imports;
    private final InstallationService installations;
    private final SpecContentService specContent;
    private final WebhookVerifier webhookVerifier;
    private final WebhookService webhooks;

    @Override
    public ForgeInstallationList listForgeInstallations() {
        return installations.list();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Scan startRepositoryScan(final ScanRequest request) {
        return scans.start(request);
    }

    @Override
    public Scan getRepositoryScan(final UUID scanId) {
        return scans.get(scanId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Connection createRepositoryConnection(final ConnectionRequest request) {
        return connections.create(request);
    }

    @Override
    public ConnectionList listRepositoryConnections() {
        return connections.list();
    }

    @Override
    public Connection getRepositoryConnection(final UUID connectionId) {
        return connections.get(connectionId);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ImportRun startImport(final UUID connectionId) {
        return imports.startManual(connectionId);
    }

    @Override
    public ImportRunList listImportRuns(final UUID connectionId) {
        return imports.list(connectionId);
    }

    @Override
    public ImportRun getImportRun(final UUID connectionId, final UUID runId) {
        return imports.get(connectionId, runId);
    }

    /**
     * Verified before anything is parsed: the signature covers the exact bytes GitHub sent, and an
     * unverified delivery is an unauthenticated trigger for imports and outbound writes.
     */
    @Override
    public void receiveGitHubWebhook(final String body, final String event, final String deliveryId,
            final String signature) {
        if (!webhookVerifier.verify(body, signature)) {
            throw Problems.unauthorized("The webhook signature did not verify.");
        }
        webhooks.handle(event, deliveryId, body);
    }

    @Override
    public void updateSpecContent(final UUID specId, final SpecContentUpdate update) {
        specContent.refuseEdit(specId);
    }
}
