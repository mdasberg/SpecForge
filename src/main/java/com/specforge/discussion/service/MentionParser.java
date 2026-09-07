package com.specforge.discussion.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pulls {@code @handle} out of a comment body. A false positive inside a code span still resolves
 * against real members before it notifies anyone, so the cost of not being markdown-aware here is a
 * handle-shaped word rendering as a link rather than a wrong notification.
 */
final class MentionParser {

    private static final Pattern HANDLE = Pattern.compile("(?<![\\w@])@([a-zA-Z0-9._-]+)");

    private MentionParser() {}

    static Set<String> handles(final String body) {
        final Set<String> handles = new LinkedHashSet<>();
        final Matcher matcher = HANDLE.matcher(body);
        while (matcher.find()) {
            handles.add(matcher.group(1));
        }
        return handles;
    }
}
