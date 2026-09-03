package com.specforge.repository.internal;

import com.specforge.platform.api.dto.Connection;
import com.specforge.platform.api.dto.ConnectionList;
import com.specforge.platform.api.dto.ConnectionRequest;
import com.specforge.platform.api.dto.ForgeInstallationList;
import com.specforge.platform.api.dto.ImportRun;
import com.specforge.platform.api.dto.ImportRunList;
import com.specforge.platform.api.dto.ProjectConfiguration;
import com.specforge.platform.api.dto.Scan;
import com.specforge.platform.api.dto.ScanRequest;
import com.specforge.platform.api.dto.SpecContentUpdate;
import com.specforge.platform.api.generated.RepositoryApi;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * The repository capability's HTTP surface. It implements the interface generated from
 * {@code specforge-api.yaml}, so a change to the contract that this does not follow fails to
 * compile rather than reaching a client.
 */
@RestController
class RepositoryController implements RepositoryApi {

    private final ConnectionService connections;
    private final ScanService scans;
    private final ImportService imports;
    private final InstallationService installations;
    private final SpecContentService specContent;
    private final WebhookVerifier webhookVerifier;
    private final WebhookService webhooks;

    RepositoryController(
            ConnectionService connections,
            ScanService scans,
            ImportService imports,
            InstallationService installations,
            SpecContentService specContent,
            WebhookVerifier webhookVerifier,
            WebhookService webhooks) {
        this.connections = connections;
        this.scans = scans;
        this.imports = imports;
        this.installations = installations;
        this.specContent = specContent;
        this.webhookVerifier = webhookVerifier;
        this.webhooks = webhooks;
    }

    @Override
    public ForgeInstallationList listForgeInstallations() {
        return new ForgeInstallationList(
                installations.list().stream().map(RepositoryMapper::installation).toList());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Scan startRepositoryScan(ScanRequest request) {
        RepositoryScanEntity scan = scans.start(
                request.getInstallationId(),
                request.getRepositoryFullName(),
                request.getBranch(),
                request.getPathGlob(),
                SpecFileFormat.valueOf(request.getSpecFormat().name()));
        return RepositoryMapper.scan(scan, List.of());
    }

    @Override
    public Scan getRepositoryScan(UUID scanId) {
        RepositoryScanEntity scan = scans.get(scanId);
        return RepositoryMapper.scan(scan, scans.files(scanId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Connection createRepositoryConnection(ConnectionRequest request) {
        ProjectConfiguration project = request.getProject();
        RepositoryConnectionEntity connection = connections.create(new ConnectionCommand(
                request.getInstallationId(),
                request.getRepositoryFullName(),
                request.getBranch(),
                request.getPathGlob(),
                SpecFileFormat.valueOf(request.getSpecFormat().name()),
                SyncPolicy.valueOf(request.getSyncMode().name()),
                request.getScanId(),
                new ProjectCommand(
                        project.getName(),
                        project.getTeam(),
                        project.getDomains(),
                        project.getTracker() == null ? null : project.getTracker().name(),
                        project.getTrackerProjectKey(),
                        project.getApprovalRule().getMinApprovals(),
                        project.getApprovalRule().getRequiredRoles())));
        return RepositoryMapper.connection(connection, project.getName());
    }

    @Override
    public ConnectionList listRepositoryConnections() {
        return new ConnectionList(connections.list().stream()
                .map(connection -> RepositoryMapper.connection(connection, connections.project(connection).name()))
                .toList());
    }

    @Override
    public Connection getRepositoryConnection(UUID connectionId) {
        RepositoryConnectionEntity connection = connections.get(connectionId);
        return RepositoryMapper.connection(connection, connections.project(connection).name());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ImportRun startImport(UUID connectionId) {
        ImportRunEntity run = imports.start(connections.get(connectionId), ImportTrigger.MANUAL, null);
        return RepositoryMapper.importRun(run, List.of());
    }

    @Override
    public ImportRunList listImportRuns(UUID connectionId) {
        connections.get(connectionId);
        return new ImportRunList(imports.list(connectionId).stream()
                .map(run -> RepositoryMapper.importRun(run, imports.files(run.id())))
                .toList());
    }

    @Override
    public ImportRun getImportRun(UUID connectionId, UUID runId) {
        ImportRunEntity run = imports.get(connectionId, runId);
        return RepositoryMapper.importRun(run, imports.files(run.id()));
    }

    /**
     * Verified before anything is parsed: the signature covers the exact bytes GitHub sent, and an
     * unverified delivery is an unauthenticated trigger for imports and outbound writes.
     */
    @Override
    public void receiveGitHubWebhook(String body, String event, String deliveryId, String signature) {
        if (!webhookVerifier.verify(body, signature)) {
            throw Problems.unauthorized("The webhook signature did not verify.");
        }
        webhooks.handle(event, deliveryId, body);
    }

    @Override
    public void updateSpecContent(UUID specId, SpecContentUpdate update) {
        specContent.refuseEdit(specId);
    }
}
