package com.specforge.repository.service;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/** Which files in a repository count as specifications, and what their path says about them. */
public final class SpecPaths {

    private SpecPaths() {}

    static List<String> matching(final List<String> paths, final String glob) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return paths.stream().filter(path -> matcher.matches(Path.of(path))).sorted().toList();
    }

    static boolean matches(final String path, final String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(Path.of(path));
    }

    /**
     * The directory holding the file is the domain — for {@code openspec/specs/billing/spec.md}
     * that is {@code billing}. It is a convention rather than a rule, so a path with nothing to
     * take it from simply has no domain.
     */
    static String domainOf(final String path) {
        final Path parent = Path.of(path).getParent();
        return parent == null ? null : parent.getFileName().toString();
    }
}
