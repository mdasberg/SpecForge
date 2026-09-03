package com.specforge.repository.service;

import com.specforge.repository.configuration.GitHubProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The webhook endpoint is the one route on the API that no Keycloak token reaches, so the HMAC
 * signature is the whole of its authentication. An unconfigured secret rejects every delivery
 * rather than accepting unsigned ones: an unverified delivery is an unauthenticated trigger for
 * imports and outbound status writes.
 */
@RequiredArgsConstructor
@Component
public class WebhookVerifier {

    private final GitHubProperties properties;

    public boolean verify(final String rawBody, final String signatureHeader) {
        final String configured = properties.webhookSecret();
        if (configured == null || configured.isBlank() || signatureHeader == null) {
            return false;
        }
        if (!signatureHeader.startsWith("sha256=")) {
            return false;
        }
        final byte[] expected = hmac(configured, rawBody);
        byte[] presented;
        try {
            presented = HexFormat.of().parseHex(signatureHeader.substring("sha256=".length()));
        } catch (final IllegalArgumentException e) {
            return false;
        }
        // Constant-time: a byte-by-byte comparison leaks how much of a forged signature was right.
        return MessageDigest.isEqual(expected, presented);
    }

    private static byte[] hmac(final String secret, final String body) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (final java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }
}
