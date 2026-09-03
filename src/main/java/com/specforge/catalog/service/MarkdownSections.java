package com.specforge.catalog.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Splits a version's content into the sections a discussion can anchor to.
 *
 * <p>A section's key is its heading slug plus the ordinal of that slug in the document, never a
 * line number. Line numbers move on every edit above them; a slug moves only when the heading
 * itself is rewritten, and when that happens the old key is simply absent from the new version —
 * the anchor goes stale honestly instead of silently reattaching to different text.
 *
 * <p>Only ATX headings ({@code # Heading}) are recognised. Setext underlining is not: the
 * specification formats SpecForge imports use ATX throughout, and guessing at underlines would
 * turn a line of dashes in a table into a heading.
 */
public final class MarkdownSections {

    private MarkdownSections() {}

    static List<ParsedSection> parse(final String normalisedContent) {
        final String[] lines = normalisedContent.isEmpty() ? new String[0] : normalisedContent.split("\n", -1);
        final List<ParsedSection> sections = new ArrayList<>();
        final List<Integer> levels = new ArrayList<>();
        final Map<String, Integer> slugCounts = new HashMap<>();
        // Indices of the enclosing sections, outermost first, so a parent is found by level alone.
        final Deque<Integer> open = new ArrayDeque<>();
        final List<Integer> endLines = new ArrayList<>();
        boolean inFence = false;
        String fence = null;

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            final String trimmed = line.strip();
            if (inFence) {
                if (fence != null && trimmed.startsWith(fence)) {
                    inFence = false;
                    fence = null;
                }
                continue;
            }
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inFence = true;
                fence = trimmed.substring(0, 3);
                continue;
            }
            final int level = headingLevel(line);
            if (level == 0) {
                continue;
            }
            final String heading = line.substring(level).strip();
            if (heading.isEmpty()) {
                continue;
            }
            final String slug = slug(heading);
            final int ordinal = slugCounts.merge(slug, 1, Integer::sum);

            // Everything still open at this level or deeper ends on the line before this heading.
            while (!open.isEmpty() && levels.get(open.peek()) >= level) {
                endLines.set(open.pop(), i);
            }
            final int parentIndex = open.isEmpty() ? -1 : open.peek();
            sections.add(new ParsedSection(slug + "-" + ordinal, heading, level, ordinal, i + 1, i + 1, parentIndex));
            levels.add(level);
            endLines.add(lines.length);
            open.push(sections.size() - 1);
        }
        while (!open.isEmpty()) {
            endLines.set(open.pop(), lines.length);
        }

        final List<ParsedSection> closed = new ArrayList<>(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            final ParsedSection section = sections.get(i);
            closed.add(new ParsedSection(
                    section.anchorKey(),
                    section.heading(),
                    section.level(),
                    section.ordinal(),
                    section.startLine(),
                    Math.max(section.startLine(), endLines.get(i)),
                    section.parentIndex()));
        }
        return closed;
    }

    /** The first heading, which is what a document's title is taken from. */
    static String title(final List<ParsedSection> sections) {
        return sections.isEmpty() ? null : sections.getFirst().heading();
    }

    private static int headingLevel(final String line) {
        int hashes = 0;
        while (hashes < line.length() && line.charAt(hashes) == '#') {
            hashes++;
        }
        if (hashes == 0 || hashes > 6 || hashes == line.length() || line.charAt(hashes) != ' ') {
            return 0;
        }
        return hashes;
    }

    private static String slug(final String heading) {
        final StringBuilder slug = new StringBuilder(heading.length());
        for (final char c : heading.toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                slug.append(c);
            } else if (!slug.isEmpty() && slug.charAt(slug.length() - 1) != '-') {
                slug.append('-');
            }
        }
        while (!slug.isEmpty() && slug.charAt(slug.length() - 1) == '-') {
            slug.setLength(slug.length() - 1);
        }
        return slug.isEmpty() ? "section" : slug.toString();
    }
}
