package com.specforge.repository.internal.forge;

import org.springframework.stereotype.Component;

/**
 * The webhook secret, exposed on its own so the verifier can read it without being handed the
 * App's private key as well.
 */
@Component
public class GitHubWebhookSecret {

    private final GitHubProperties properties;

    GitHubWebhookSecret(GitHubProperties properties) {
        this.properties = properties;
    }

    public String value() {
        return properties.webhookSecret();
    }
}
