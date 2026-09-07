package com.specforge.agent.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The model behind the model-backed checks. Absent in development: without an API key those checks
 * report skipped rather than passing, because a check that never ran has found nothing and says so.
 *
 * @param apiKey the model provider's key; the one secret here, so it is never logged or returned
 * @param model the model identity recorded on every run it produces
 */
@ConfigurationProperties(prefix = "specforge.agent")
public record AgentProperties(String apiKey, String model, String apiBaseUrl, Duration timeout) {

    public AgentProperties {
        model = model == null || model.isBlank() ? "claude-sonnet-5" : model;
        apiBaseUrl = apiBaseUrl == null || apiBaseUrl.isBlank()
                ? "https://api.anthropic.com"
                : apiBaseUrl.replaceAll("/+$", "");
        timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
    }

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
