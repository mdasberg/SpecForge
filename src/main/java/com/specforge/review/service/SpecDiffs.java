package com.specforge.review.service;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import com.specforge.platform.api.dto.DiffChange;
import com.specforge.platform.api.dto.DiffLine;
import com.specforge.platform.api.dto.DiffLineType;
import com.specforge.platform.api.dto.DiffSection;
import com.specforge.platform.api.dto.DiffSummary;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The two-level diff: sections classified against each other, then a line diff inside each section
 * that changed.
 *
 * <p>Reviewers reason in sections — "the API contract changed" — not in line ranges, and a
 * specification's headings are its structure. Classifying first is what makes the jump list
 * possible; the line diff inside a modified section carries the detail. The consequence is that a
 * one-word edit marks a whole section modified, which is the intended granularity for navigation.
 *
 * <p>A section is compared on its <em>own</em> body — the lines from its heading down to the next
 * heading of any level — rather than on everything it encloses. Comparing the enclosing range would
 * report a parent as modified whenever any descendant changed, and a reviewer would be sent to the
 * top of the document for an edit three headings down.
 *
 * <p>The result is a pure function of the two contents, which is what lets it be cached forever
 * under the pair of content hashes: versions are immutable, so a diff never needs invalidating.
 * Attribution (who changed a section and when) deliberately is not computed here — it comes from the
 * head version, not from the text, and would poison a cache shared by every review over the same
 * content.
 */
public final class SpecDiffs {

    /**
     * The key for content that precedes the first heading. It is the same empty key the search index
     * uses for "the document itself", and it exists so an edit up there is reported rather than
     * silently belonging to no section at all.
     */
    private static final String PREAMBLE_KEY = "";

    private static final String PREAMBLE_HEADING = "(preamble)";

    private SpecDiffs() {}

    public static List<DiffSection> compute(final SpecText base, final SpecText head) {
        final Map<String, Body> baseBodies = bodies(base);
        final Map<String, Body> headBodies = bodies(head);
        final List<String> baseKeys = List.copyOf(baseBodies.keySet());

        final List<DiffSection> sections = new ArrayList<>();
        int basePointer = 0;
        for (final Body headBody : headBodies.values()) {
            final int inBase = baseKeys.indexOf(headBody.anchorKey());
            if (inBase < 0) {
                sections.add(wholeSection(headBody, DiffChange.ADDED));
                continue;
            }
            // A section that survived in the head fixes where the sections the head dropped used to
            // sit, so a removal keeps its place in the jump list instead of collecting at the end.
            for (int i = basePointer; i < inBase; i++) {
                final Body dropped = baseBodies.get(baseKeys.get(i));
                if (!headBodies.containsKey(dropped.anchorKey())) {
                    sections.add(wholeSection(dropped, DiffChange.REMOVED));
                }
            }
            basePointer = inBase + 1;
            sections.add(compare(baseBodies.get(headBody.anchorKey()), headBody));
        }
        for (int i = basePointer; i < baseKeys.size(); i++) {
            final Body dropped = baseBodies.get(baseKeys.get(i));
            if (!headBodies.containsKey(dropped.anchorKey())) {
                sections.add(wholeSection(dropped, DiffChange.REMOVED));
            }
        }
        return sections;
    }

    public static DiffSummary summarise(final List<DiffSection> sections) {
        int added = 0;
        int removed = 0;
        int modified = 0;
        int changedLines = 0;
        for (final DiffSection section : sections) {
            switch (section.getChange()) {
                case ADDED -> added++;
                case REMOVED -> removed++;
                case MODIFIED -> modified++;
                case UNCHANGED -> { }
            }
            changedLines += section.getChangedLines();
        }
        return new DiffSummary(added, removed, modified, changedLines);
    }

    /** A section that exists on one side only: every line of it is a change. */
    private static DiffSection wholeSection(final Body body, final DiffChange change) {
        final boolean isAddition = change == DiffChange.ADDED;
        final List<DiffLine> lines = new ArrayList<>(body.lines().size());
        for (int i = 0; i < body.lines().size(); i++) {
            final DiffLine line = new DiffLine(
                    isAddition ? DiffLineType.ADDED : DiffLineType.REMOVED, body.lines().get(i), List.of());
            if (isAddition) {
                line.setHeadLine(body.startLine() + i);
            } else {
                line.setBaseLine(body.startLine() + i);
            }
            lines.add(line);
        }
        return new DiffSection(
                body.anchorKey(), body.heading(), body.level(), change, lines.size(), lines);
    }

    private static DiffSection compare(final Body base, final Body head) {
        if (base.lines().equals(head.lines())) {
            // Unchanged sections ship no lines: the body is already in the document view, and
            // repeating it here would double the payload for content nobody is reviewing.
            return new DiffSection(
                    head.anchorKey(), head.heading(), head.level(), DiffChange.UNCHANGED, 0, List.of());
        }
        final List<DiffLine> lines = lineDiff(base, head);
        final int changed = (int) lines.stream().filter(line -> line.getType() != DiffLineType.CONTEXT).count();
        return new DiffSection(
                head.anchorKey(), head.heading(), head.level(), DiffChange.MODIFIED, changed, lines);
    }

    private static List<DiffLine> lineDiff(final Body base, final Body head) {
        final List<DiffLine> lines = new ArrayList<>();
        int cursor = 0;
        int baseLine = base.startLine();
        int headLine = head.startLine();

        for (final AbstractDelta<String> delta : DiffUtils.diff(base.lines(), head.lines()).getDeltas()) {
            while (cursor < delta.getSource().getPosition()) {
                final DiffLine context = new DiffLine(DiffLineType.CONTEXT, base.lines().get(cursor), List.of());
                context.setBaseLine(baseLine++);
                context.setHeadLine(headLine++);
                lines.add(context);
                cursor++;
            }
            final List<String> removed = delta.getSource().getLines();
            final List<String> added = delta.getTarget().getLines();
            final List<DiffLine> removedLines = new ArrayList<>(removed.size());
            final List<DiffLine> addedLines = new ArrayList<>(added.size());
            for (final String text : removed) {
                final DiffLine line = new DiffLine(DiffLineType.REMOVED, text, List.of());
                line.setBaseLine(baseLine++);
                removedLines.add(line);
            }
            for (final String text : added) {
                final DiffLine line = new DiffLine(DiffLineType.ADDED, text, List.of());
                line.setHeadLine(headLine++);
                addedLines.add(line);
            }
            // Lines are paired by their position within the delta. A rewrite of three lines into
            // three is the case worth highlighting word by word; where the counts differ, the
            // surplus lines are wholly new or wholly gone and a word highlight would say nothing.
            for (int i = 0; i < Math.min(removedLines.size(), addedLines.size()); i++) {
                final WordDiff.Ranges ranges = WordDiff.of(removed.get(i), added.get(i));
                removedLines.get(i).setWords(ranges.base());
                addedLines.get(i).setWords(ranges.head());
            }
            lines.addAll(removedLines);
            lines.addAll(addedLines);
            cursor += removed.size();
        }
        while (cursor < base.lines().size()) {
            final DiffLine context = new DiffLine(DiffLineType.CONTEXT, base.lines().get(cursor), List.of());
            context.setBaseLine(baseLine++);
            context.setHeadLine(headLine++);
            lines.add(context);
            cursor++;
        }
        return lines;
    }

    /** One section's own lines, keyed by anchor, in document order. */
    private static Map<String, Body> bodies(final SpecText text) {
        final List<String> allLines = lines(text.content());
        final List<SpecSectionRange> sections = text.sections();
        final Map<String, Body> bodies = new LinkedHashMap<>();

        final int firstHeading = sections.isEmpty() ? allLines.size() + 1 : sections.getFirst().startLine();
        final List<String> preamble = slice(allLines, 1, firstHeading - 1);
        if (preamble.stream().anyMatch(line -> !line.isBlank())) {
            bodies.put(PREAMBLE_KEY, new Body(PREAMBLE_KEY, PREAMBLE_HEADING, 1, 1, preamble));
        }
        for (int i = 0; i < sections.size(); i++) {
            final SpecSectionRange section = sections.get(i);
            final int ownEnd = i + 1 < sections.size() ? sections.get(i + 1).startLine() - 1 : allLines.size();
            bodies.put(section.anchorKey(), new Body(
                    section.anchorKey(),
                    section.heading(),
                    section.level(),
                    section.startLine(),
                    slice(allLines, section.startLine(), ownEnd)));
        }
        return bodies;
    }

    /** Both bounds are 1-based and inclusive, which is how a section states its range. */
    private static List<String> slice(final List<String> lines, final int from, final int to) {
        return from > to ? List.of() : List.copyOf(lines.subList(from - 1, Math.min(to, lines.size())));
    }

    private static List<String> lines(final String content) {
        if (content.isEmpty()) {
            return List.of();
        }
        // Normalised content always ends in a newline, and splitting on it would otherwise add an
        // empty line that is not in the document.
        final String body = content.endsWith("\n") ? content.substring(0, content.length() - 1) : content;
        return List.of(body.split("\n", -1));
    }

    /** A section's heading plus the lines it owns, which is what gets compared. */
    private record Body(String anchorKey, String heading, int level, int startLine, List<String> lines) {}
}
