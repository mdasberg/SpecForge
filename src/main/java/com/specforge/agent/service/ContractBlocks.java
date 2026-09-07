package com.specforge.agent.service;

import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The fenced blocks a specification uses to state an API contract, and what disappeared from one
 * between two versions.
 *
 * <p>Only sections whose heading names an API or a contract are read. A specification is prose with
 * examples in it, and treating every fenced block as a contract would report a reworded sample
 * request as a breaking change — which trains reviewers to ignore the check.
 */
final class ContractBlocks {

    private static final String FENCE = "```";

    private ContractBlocks() {}

    /** Contract-block lines per section anchor key, in document order. */
    static Map<String, List<String>> bySection(final SpecText text) {
        final Map<String, List<String>> blocks = new LinkedHashMap<>();
        for (final SpecSectionRange section : text.sections()) {
            if (!namesAContract(SpecSections.title(section))) {
                continue;
            }
            final List<String> lines = fenced(SpecSections.body(text, section));
            if (!lines.isEmpty()) {
                blocks.put(section.anchorKey(), lines);
            }
        }
        return blocks;
    }

    /**
     * Lines the base declared and the head no longer does.
     *
     * <p>ponytail: a line-level comparison, not a schema-aware one — it catches a removed field or
     * a removed status code, which is what breaks a consumer, and says nothing about a type that
     * narrowed. Parse the block as OpenAPI or JSON Schema if that stops being enough.
     */
    static List<String> removed(final List<String> baseLines, final List<String> headLines) {
        final Set<String> head = new LinkedHashSet<>(headLines);
        final List<String> removed = new ArrayList<>();
        for (final String line : baseLines) {
            if (!head.contains(line)) {
                removed.add(line);
            }
        }
        return List.copyOf(removed);
    }

    private static boolean namesAContract(final String heading) {
        final String lower = heading.toLowerCase(Locale.ROOT);
        return lower.contains("api") || lower.contains("contract") || lower.contains("endpoint");
    }

    /** The significant lines inside fenced blocks: the fences themselves and blank lines are noise. */
    private static List<String> fenced(final List<String> lines) {
        final List<String> inside = new ArrayList<>();
        boolean open = false;
        for (final String line : lines) {
            if (line.stripLeading().startsWith(FENCE)) {
                open = !open;
                continue;
            }
            if (open && !line.isBlank()) {
                inside.add(line.strip());
            }
        }
        return List.copyOf(inside);
    }
}
