package com.specforge.catalog.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * A version's identity is the sha256 of its normalised content, not the commit it arrived on.
 * Normalising first means a file rewritten with CRLF endings, or one whose editor trimmed a
 * trailing space, is recognised as unchanged instead of creating a version nobody asked for.
 */
final class SpecContent {

    private SpecContent() {}

    /** Line endings to LF, trailing whitespace off every line, exactly one closing newline. */
    static String normalise(String raw) {
        String[] lines = raw.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        int lastContentLine = -1;
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].stripTrailing();
            if (!lines[i].isEmpty()) {
                lastContentLine = i;
            }
        }
        StringBuilder normalised = new StringBuilder(raw.length());
        for (int i = 0; i <= lastContentLine; i++) {
            normalised.append(lines[i]).append('\n');
        }
        return normalised.toString();
    }

    static String sha256(String content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every JVM ships SHA-256; if this one does not, nothing downstream can be trusted.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
