package com.specforge.repository.service;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/** Which files in a repository count as specifications, and what their path says about them. */
public final class SpecPaths {

    /** The directory a repository keeps its specifications under, by convention. */
    private static final String SPECS = "specs";

    private SpecPaths() {}

    static List<String> matching(final List<String> paths, final String glob) {
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return paths.stream().filter(path -> matcher.matches(Path.of(path))).sorted().toList();
    }

    static boolean matches(final String path, final String glob) {
        return FileSystems.getDefault().getPathMatcher("glob:" + glob).matches(Path.of(path));
    }

    /**
     * The segment directly under the {@code specs} directory is the domain — {@code billing} for
     * {@code openspec/specs/billing/spec.md}, and {@code clm} for
     * {@code openspecs/specs/clm/claim/spec.md}. Taking the segment after {@code specs} rather than
     * the directory holding the file is what makes the two depths agree: a repository that groups
     * its specifications one level deeper would otherwise report every capability as its own
     * domain, which is a domain per specification and no grouping at all.
     *
     * <p>It stays a convention rather than a rule. A path with no {@code specs} directory falls
     * back to the directory holding the file, and a path with nothing to take it from — a file at
     * the root, or one sitting directly in {@code specs} — has no domain.
     */
    static String domainOf(final String path) {
        final Path filePath = Path.of(path);
        final Path parent = filePath.getParent();
        if (parent == null) {
            return null;
        }
        // The last `specs`, not the first: a repository is free to nest one inside another
        // directory of its own, and the innermost is the one the specifications actually sit under.
        for (int segment = parent.getNameCount() - 1; segment >= 0; segment--) {
            if (SPECS.equals(parent.getName(segment).toString()) && segment + 1 < parent.getNameCount()) {
                return parent.getName(segment + 1).toString();
            }
        }
        // A file sitting directly in `specs` has no segment to name a domain with; `specs` itself
        // is the container, not a domain.
        final String directory = parent.getFileName().toString();
        return SPECS.equals(directory) ? null : directory;
    }
}
