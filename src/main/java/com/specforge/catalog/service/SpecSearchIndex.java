package com.specforge.catalog.service;

import com.specforge.catalog.entity.SpecSearchRow;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Turns a version into the rows search reads. Two things make this its own step rather than a
 * query over {@code spec_section}: a section stores a line range and not its text, and the
 * document's title and path have to be findable through the same index as its body, which they
 * are not part of.
 */
final class SpecSearchIndex {

    /** The anchor of the row standing for the document itself, which has no heading of its own. */
    private static final String DOCUMENT_ANCHOR = "";

    private SpecSearchIndex() {}

    /**
     * A section is indexed with its own text only — the lines up to the next heading — and not with
     * everything it encloses. Indexing the enclosed text too would make the outermost section the
     * best match for every term in the document, which is the opposite of naming where a term is.
     */
    static List<SpecSearchRow> rowsOf(
            final UUID documentId,
            final UUID versionId,
            final String title,
            final String path,
            final String content,
            final List<ParsedSection> sections) {
        final String[] lines = content.isEmpty() ? new String[0] : content.split("\n", -1);
        final List<SpecSearchRow> rows = new ArrayList<>(sections.size() + 1);
        rows.add(new SpecSearchRow(UUID.randomUUID(), documentId, versionId, DOCUMENT_ANCHOR, title, path));
        for (int i = 0; i < sections.size(); i++) {
            final ParsedSection section = sections.get(i);
            // startLine is 1-based and names the heading itself, so the body starts on the next
            // line; it runs to the line before the following heading, whatever level that is.
            final int from = section.startLine();
            final int to = i + 1 < sections.size() ? sections.get(i + 1).startLine() - 1 : lines.length;
            rows.add(new SpecSearchRow(
                    UUID.randomUUID(),
                    documentId,
                    versionId,
                    section.anchorKey(),
                    section.heading(),
                    join(lines, from, to)));
        }
        return rows;
    }

    private static String join(final String[] lines, final int from, final int to) {
        final StringBuilder body = new StringBuilder();
        for (int line = from; line < to && line < lines.length; line++) {
            body.append(lines[line]).append('\n');
        }
        return body.toString();
    }
}
