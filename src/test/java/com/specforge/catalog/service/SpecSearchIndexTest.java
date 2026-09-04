package com.specforge.catalog.service;

import com.specforge.catalog.entity.SpecSearchRow;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down how sections and their content are flattened into search rows: a document row for the
 * title and path, then one row per section carrying only its own text. A section's text must not
 * include the text of nested sections — doing so makes the outermost section the best match for
 * every term in the document, hiding where the term actually appears.
 */
class SpecSearchIndexTest {

    @Test
    void firstRowRepresentsTheDocumentItselfWithEmptyAnchor() {
        final String content = """
                # Heading
                
                Body text.
                """;
        final List<ParsedSection> sections = MarkdownSections.parse(content);
        final UUID documentId = UUID.randomUUID();
        final UUID versionId = UUID.randomUUID();
        final String title = "My Title";
        final String path = "/spec/path";

        final List<SpecSearchRow> rows = SpecSearchIndex.rowsOf(documentId, versionId, title, path, content, sections);

        assertThat(rows.getFirst())
                .satisfies(row -> {
                    assertThat(row.anchorKey()).isEmpty();
                    assertThat(row.heading()).isEqualTo(title);
                    assertThat(row.body()).isEqualTo(path);
                    assertThat(row.documentId()).isEqualTo(documentId);
                    assertThat(row.versionId()).isEqualTo(versionId);
                });
    }

    @Test
    void oneRowPerSectionInDocumentOrder() {
        final String content = """
                # Billing
                
                ## Validation Rules
                
                A policy must exist.
                
                ## Preconditions
                
                The member is covered.
                """;
        final List<ParsedSection> sections = MarkdownSections.parse(content);
        final UUID documentId = UUID.randomUUID();
        final UUID versionId = UUID.randomUUID();

        final List<SpecSearchRow> rows = SpecSearchIndex.rowsOf(documentId, versionId, "Title", "/path", content, sections);

        assertThat(rows).hasSize(4);
        assertThat(rows)
                .extracting(SpecSearchRow::anchorKey)
                .containsExactly("", "billing-1", "validation-rules-1", "preconditions-1");
        assertThat(rows)
                .extracting(SpecSearchRow::heading)
                .containsExactly("Title", "Billing", "Validation Rules", "Preconditions");
        assertThat(rows).allMatch(row -> row.documentId().equals(documentId));
        assertThat(rows).allMatch(row -> row.versionId().equals(versionId));
    }

    @Test
    void sectionIsIndexedWithItsOwnTextOnly() {
        final String content = """
                # Billing
                
                ## Validation Rules
                
                A policy must exist.
                
                ## Preconditions
                
                The member is covered.
                """;
        final List<ParsedSection> sections = MarkdownSections.parse(content);

        final List<SpecSearchRow> rows = SpecSearchIndex.rowsOf(UUID.randomUUID(), UUID.randomUUID(), "Title", "/path", content, sections);

        final SpecSearchRow billingRow = rows.get(1);
        assertThat(billingRow.body()).doesNotContain("A policy must exist").doesNotContain("The member is covered");

        final SpecSearchRow validationRow = rows.get(2);
        assertThat(validationRow.body()).contains("A policy must exist").doesNotContain("The member is covered");

        final SpecSearchRow preconditionsRow = rows.get(3);
        assertThat(preconditionsRow.body()).doesNotContain("A policy must exist").contains("The member is covered");
    }

    @Test
    void lastSectionRunsToEndOfContent() {
        final String content = """
                # Billing
                
                ## Preconditions
                
                The member is covered.
                Last line.
                """;
        final List<ParsedSection> sections = MarkdownSections.parse(content);

        final List<SpecSearchRow> rows = SpecSearchIndex.rowsOf(UUID.randomUUID(), UUID.randomUUID(), "Title", "/path", content, sections);

        final SpecSearchRow preconditionsRow = rows.get(2);
        assertThat(preconditionsRow.body()).contains("The member is covered").contains("Last line");
    }

    @Test
    void contentWithNoHeadingProducesOnlyDocumentRow() {
        final String content = """
                Just prose without any headings.
                More prose.
                """;
        final List<ParsedSection> sections = MarkdownSections.parse(content);
        final UUID documentId = UUID.randomUUID();
        final UUID versionId = UUID.randomUUID();

        final List<SpecSearchRow> rows = SpecSearchIndex.rowsOf(documentId, versionId, "Title", "/path", content, sections);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst())
                .satisfies(row -> {
                    assertThat(row.anchorKey()).isEmpty();
                    assertThat(row.heading()).isEqualTo("Title");
                    assertThat(row.body()).isEqualTo("/path");
                    assertThat(row.documentId()).isEqualTo(documentId);
                    assertThat(row.versionId()).isEqualTo(versionId);
                });
    }
}
