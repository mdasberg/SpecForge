package com.specforge.repository.service;

import com.specforge.repository.entity.ConnectionState;
import com.specforge.repository.entity.ImportTrigger;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.entity.SyncPolicy;
import com.specforge.repository.entity.WebhookDeliveryEntity;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import com.specforge.repository.repository.WebhookDeliveryRepository;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Turns a verified GitHub delivery into the synchronisation it implies.
 *
 * <p>Deliveries are recorded by id and a repeat is acknowledged without being acted on. GitHub
 * redelivers on any doubt about the response, and an import triggered twice would be harmless but
 * a status write and a proposal update would not be.
 */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookDeliveryRepository deliveries;
    private final RepositoryConnectionRepository connections;
    private final InstallationService installationService;
    private final ProposalService proposals;
    private final ImportService imports;
    private final ObjectMapper json;
    private final Clock clock;

    WebhookService(
            WebhookDeliveryRepository deliveries,
            RepositoryConnectionRepository connections,
            InstallationService installationService,
            ProposalService proposals,
            ImportService imports,
            ObjectMapper json,
            Clock clock) {
        this.deliveries = deliveries;
        this.connections = connections;
        this.installationService = installationService;
        this.proposals = proposals;
        this.imports = imports;
        this.json = json;
        this.clock = clock;
    }

    public void handle(String event, String deliveryId, String rawBody) {
        if (deliveryId != null && !recordDelivery(event, deliveryId)) {
            log.debug("Ignoring redelivered {} webhook {}", event, deliveryId);
            return;
        }
        JsonNode payload;
        try {
            payload = json.readTree(rawBody);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("The webhook payload is not JSON.");
        }
        switch (event == null ? "" : event) {
            case "installation", "installation_repositories" -> handleInstallation(payload);
            case "push" -> handlePush(payload);
            case "pull_request" -> handlePullRequest(payload);
            default -> log.debug("Ignoring unhandled webhook event {}", event);
        }
    }

    /** @return false when this delivery has already been processed */
    @Transactional
    public boolean recordDelivery(String event, String deliveryId) {
        if (deliveries.existsById(deliveryId)) {
            return false;
        }
        // The event header is optional in the contract, and the column is not nullable: an
        // unnamed delivery is still a delivery that must not be replayed.
        deliveries.save(new WebhookDeliveryEntity(deliveryId, event == null ? "unknown" : event, clock.instant()));
        return true;
    }

    private void handleInstallation(JsonNode payload) {
        String externalId = payload.path("installation").path("id").asText(null);
        if (externalId == null) {
            return;
        }
        if ("deleted".equals(payload.path("action").asText())) {
            installationService.revoke(externalId);
        } else {
            installationService.refresh(externalId);
        }
    }

    private void handlePush(JsonNode payload) {
        String ref = payload.path("ref").asText("");
        if (!ref.startsWith("refs/heads/")) {
            return;
        }
        String branch = ref.substring("refs/heads/".length());
        String repositoryFullName = payload.path("repository").path("full_name").asText(null);
        if (repositoryFullName == null) {
            return;
        }
        Set<String> changed = new LinkedHashSet<>();
        for (JsonNode commit : payload.path("commits")) {
            commit.path("added").forEach(path -> changed.add(path.asText()));
            commit.path("modified").forEach(path -> changed.add(path.asText()));
        }
        if (changed.isEmpty()) {
            return;
        }
        for (RepositoryConnectionEntity connection :
                connections.findByRepositoryFullNameAndBranch(repositoryFullName, branch)) {
            if (connection.syncMode() == SyncPolicy.ON_PUSH && connection.state() != ConnectionState.DEGRADED) {
                imports.start(connection, ImportTrigger.PUSH, List.copyOf(changed));
            }
        }
    }

    private void handlePullRequest(JsonNode payload) {
        JsonNode pullRequest = payload.path("pull_request");
        String repositoryFullName = payload.path("repository").path("full_name").asText(null);
        int number = payload.path("number").asInt(pullRequest.path("number").asInt());
        if (repositoryFullName == null || number == 0) {
            return;
        }
        String action = payload.path("action").asText("");
        if ("closed".equals(action)) {
            proposals.pullRequestClosed(repositoryFullName, number, pullRequest.path("merged").asBoolean(false));
            return;
        }
        if (!List.of("opened", "reopened", "synchronize", "ready_for_review").contains(action)) {
            return;
        }
        proposals.pullRequestChanged(
                repositoryFullName,
                number,
                pullRequest.path("base").path("ref").asText(null),
                pullRequest.path("head").path("sha").asText(null),
                pullRequest.path("title").asText(null),
                pullRequest.path("user").path("login").asText(null));
    }
}
