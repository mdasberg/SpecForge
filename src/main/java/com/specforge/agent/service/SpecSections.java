package com.specforge.agent.service;

import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import java.util.List;

/**
 * Reading a section's own text out of a parsed specification.
 *
 * <p>It slices the catalogue's normalised content by the line range the catalogue's own parser
 * reported, rather than re-parsing markdown: the anchor key a finding is stored against has to be
 * the key a discussion and a diff use, and a second parser would eventually disagree about which
 * lines that key covers.
 */
final class SpecSections {

    private SpecSections() {}

    /** The section's lines, its heading included, as the parser bounded it (1-based, inclusive). */
    static List<String> lines(final SpecText text, final SpecSectionRange section) {
        final List<String> all = text.content().lines().toList();
        final int from = Math.max(0, section.startLine() - 1);
        final int to = Math.min(all.size(), section.endLine());
        return from >= to ? List.of() : List.copyOf(all.subList(from, to));
    }

    /** The section's lines without its heading — what the section actually says. */
    static List<String> body(final SpecText text, final SpecSectionRange section) {
        final List<String> lines = lines(text, section);
        return lines.isEmpty() ? List.of() : List.copyOf(lines.subList(1, lines.size()));
    }

    /** The heading's text, the leading hashes stripped. */
    static String title(final SpecSectionRange section) {
        return section.heading().replaceFirst("^#+\\s*", "").trim();
    }

    /** Whether this heading names a requirement, in the OpenSpec sense of `### Requirement: X`. */
    static boolean isRequirement(final SpecSectionRange section) {
        return title(section).regionMatches(true, 0, "Requirement:", 0, "Requirement:".length());
    }

    static boolean isScenario(final SpecSectionRange section) {
        return title(section).regionMatches(true, 0, "Scenario:", 0, "Scenario:".length());
    }

    /** The sections nested inside this one, by line range rather than by resemblance. */
    static List<SpecSectionRange> nested(final SpecText text, final SpecSectionRange section) {
        return text.sections().stream()
                .filter(candidate -> candidate.level() > section.level())
                .filter(candidate -> candidate.startLine() > section.startLine()
                        && candidate.endLine() <= section.endLine())
                .toList();
    }
}
