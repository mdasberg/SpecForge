package com.specforge.repository.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The GitHub App SpecForge reads repositories through. A personal access token was rejected on
 * purpose: an App installation is granted per repository by the owning team and can be revoked by
 * them, which is the boundary the product promises.
 *
 * @param privateKey the App's PKCS#8 private key, PEM encoded; it signs the JWT that buys a
 *     short-lived installation token, so it is the one secret that must never reach a log
 * @param webhookSecret the shared secret every inbound delivery is HMAC-verified against
 */
@ConfigurationProperties(prefix = "specforge.github")
public record GitHubProperties(String appId, String privateKey, String webhookSecret, String apiBaseUrl, String statusContext) {

    public GitHubProperties {
        apiBaseUrl = apiBaseUrl == null ? "https://api.github.com" : apiBaseUrl.replaceAll("/+$", "");
        statusContext = statusContext == null ? "specforge/review" : statusContext;
    }

    public boolean configured() {
        return appId != null && !appId.isBlank() && privateKey != null && !privateKey.isBlank();
    }
}
