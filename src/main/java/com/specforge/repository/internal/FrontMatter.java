package com.specforge.repository.internal;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Reads the tags out of a YAML front-matter block when a specification has one.
 *
 * <p>Only {@code tags} is read, and only in its two common shapes, rather than pulling in a YAML
 * parser for a handful of strings: front matter is optional metadata, and a file whose front
 * matter SpecForge cannot read still imports fine without tags.
 */
final class FrontMatter {

    private FrontMatter() {}

    static Set<String> tags(String content) {
        String[] lines = content.split("\\R");
        if (lines.length == 0 || !lines[0].strip().equals("---")) {
            return Set.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        boolean inList = false;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.strip();
            if (trimmed.equals("---")) {
                break;
            }
            if (inList) {
                if (trimmed.startsWith("- ")) {
                    tags.add(unquote(trimmed.substring(2)));
                    continue;
                }
                inList = false;
            }
            if (trimmed.startsWith("tags:")) {
                String value = trimmed.substring("tags:".length()).strip();
                if (value.isEmpty()) {
                    inList = true;
                } else if (value.startsWith("[") && value.endsWith("]")) {
                    for (String tag : value.substring(1, value.length() - 1).split(",")) {
                        if (!tag.isBlank()) {
                            tags.add(unquote(tag.strip()));
                        }
                    }
                }
            }
        }
        return Set.copyOf(tags);
    }

    private static String unquote(String value) {
        String trimmed = value.strip();
        if (trimmed.length() >= 2
                && (trimmed.startsWith("\"") && trimmed.endsWith("\"") || trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
