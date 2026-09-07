package com.specforge.agent.model;

import com.specforge.agent.configuration.AgentProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The model port over the Anthropic Messages API.
 *
 * <p>The model is asked for JSON and nothing else, and anything it returns that is not the expected
 * shape is dropped rather than surfaced: a malformed answer is the model's failure, and turning it
 * into a finding a reviewer has to dispose of would make the check worse than absent. A failure of
 * any kind — no key, a refused request, unparseable output — comes back empty, which the check
 * reports as skipped.
 */
@Component
class AnthropicReviewModel implements ReviewModel {

    private static final Logger log = LoggerFactory.getLogger(AnthropicReviewModel.class);
    private static final int MAX_TOKENS = 2048;

    private static final String PROMPT = """
            You are reviewing a proposed change to a software specification. %s

            Answer with JSON only, no prose and no code fence: an array of objects, each with the
            keys "sectionKey", "severity", "title", "body" and optionally "proposedText". Use the
            sectionKey values exactly as they appear in the SECTION markers below. severity is one of
            INFO, WARNING or ERROR. Return an empty array if you find nothing worth a reviewer's
            attention; do not invent findings to fill the array.

            %s
            """;

    private final AgentProperties properties;
    private final ObjectMapper json;
    private final HttpClient http;

    AnthropicReviewModel(final AgentProperties properties, final ObjectMapper json) {
        this.properties = properties;
        this.json = json;
        this.http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
    }

    @Override
    public Optional<ModelResult> review(final ModelRequest request) {
        if (!properties.configured()) {
            return Optional.empty();
        }
        try {
            final HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(properties.apiBaseUrl() + "/v1/messages"))
                            .header("content-type", "application/json")
                            .header("x-api-key", properties.apiKey())
                            .header("anthropic-version", "2023-06-01")
                            .timeout(properties.timeout())
                            .POST(HttpRequest.BodyPublishers.ofString(body(request)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Check {} could not reach the review model: {}", request.checkKey(), response.statusCode());
                return Optional.empty();
            }
            return Optional.of(new ModelResult(properties.model(), findings(response.body())));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (final RuntimeException | java.io.IOException e) {
            log.warn("Check {} could not reach the review model", request.checkKey(), e);
            return Optional.empty();
        }
    }

    private String body(final ModelRequest request) {
        final ObjectNode message = json.createObjectNode();
        message.put("role", "user");
        message.put("content", PROMPT.formatted(request.instruction(), request.content()));
        final ArrayNode messages = json.createArrayNode();
        messages.add(message);
        final ObjectNode payload = json.createObjectNode();
        payload.put("model", properties.model());
        payload.put("max_tokens", MAX_TOKENS);
        payload.set("messages", messages);
        return json.writeValueAsString(payload);
    }

    /**
     * The findings out of the response's first text block. Every field is read defensively: this is
     * generated text, so the only thing worth assuming about its shape is that it will sometimes be
     * wrong.
     */
    private List<ModelFinding> findings(final String responseBody) {
        final JsonNode text = json.readTree(responseBody).path("content").path(0).path("text");
        if (text.isMissingNode() || text.asString("").isBlank()) {
            return List.of();
        }
        final JsonNode parsed;
        try {
            parsed = json.readTree(text.asString());
        } catch (final RuntimeException e) {
            log.warn("The review model answered with something other than JSON");
            return List.of();
        }
        if (!parsed.isArray()) {
            return List.of();
        }
        final List<ModelFinding> findings = new ArrayList<>();
        for (final JsonNode node : parsed) {
            final String sectionKey = node.path("sectionKey").asString(null);
            final String title = node.path("title").asString(null);
            if (sectionKey == null || sectionKey.isBlank() || title == null || title.isBlank()) {
                continue;
            }
            findings.add(new ModelFinding(
                    sectionKey,
                    node.path("severity").asString("INFO"),
                    title,
                    node.path("body").asString(""),
                    node.path("proposedText").asString(null)));
        }
        return List.copyOf(findings);
    }
}
