package com.specforge.repository.internal;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/** Which files in a repository count as specifications, and what their path says about them. */
final class SpecPaths {

    private SpecPaths() {}

    static List<String> matching(List<String> paths, String glob) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return paths.stream().filter(path -> matcher.matches(Path.of(path))).sorted().toList();
    }

    static boolean matches(String path, String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(Path.of(path));
    }

    /**
     * The directory holding the file is the domain — for {@code openspec/specs/billing/spec.md}
     * that is {@code billing}. It is a convention rather than a rule, so a path with nothing to
     * take it from simply has no domain.
     */
    static String domainOf(String path) {
        Path parent = Path.of(path).getParent();
        return parent == null ? null : parent.getFileName().toString();
    }
}
