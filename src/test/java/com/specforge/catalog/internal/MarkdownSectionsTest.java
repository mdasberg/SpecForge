package com.specforge.catalog.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownSectionsTest {

    @Test
    void keysEverySectionByHeadingSlugAndOrdinal() {
        List<ParsedSection> sections = MarkdownSections.parse("""
                # Claim preauthorization

                ## Purpose

                Text.

                ## API Contract

                More text.
                """);

        assertThat(sections)
                .extracting(ParsedSection::anchorKey)
                .containsExactly("claim-preauthorization-1", "purpose-1", "api-contract-1");
        assertThat(sections).extracting(ParsedSection::level).containsExactly(1, 2, 2);
        assertThat(sections.get(1).parentIndex()).isZero();
    }

    @Test
    void distinguishesRepeatedHeadingsByOrdinal() {
        List<ParsedSection> sections = MarkdownSections.parse("""
                # Spec

                ## Scenario

                ## Scenario
                """);

        assertThat(sections)
                .extracting(ParsedSection::anchorKey)
                .containsExactly("spec-1", "scenario-1", "scenario-2");
    }

    @Test
    void keepsEveryOtherKeyWhenOneSectionsBodyChanges() {
        String before = """
                # Spec

                ## Purpose

                Old.

                ## API Contract

                Same.
                """;
        String after = before.replace("Old.", "New, longer body.\n\nWith another paragraph.");

        assertThat(MarkdownSections.parse(after))
                .extracting(ParsedSection::anchorKey)
                .isEqualTo(MarkdownSections.parse(before).stream().map(ParsedSection::anchorKey).toList());
    }

    @Test
    void orphansTheKeyOfARenamedHeading() {
        List<String> renamed = MarkdownSections.parse("""
                # Spec

                ## Preconditions
                """.replace("Preconditions", "Prerequisites")).stream()
                .map(ParsedSection::anchorKey)
                .toList();

        assertThat(renamed).doesNotContain("preconditions-1").contains("prerequisites-1");
    }

    @Test
    void ignoresHashesInsideAFencedCodeBlock() {
        List<ParsedSection> sections = MarkdownSections.parse("""
                # Spec

                ```bash
                # this is a shell comment, not a heading
                ```

                ## Real
                """);

        assertThat(sections).extracting(ParsedSection::heading).containsExactly("Spec", "Real");
    }

    @Test
    void endsASectionWhereTheNextOneOfTheSameLevelBegins() {
        List<ParsedSection> sections = MarkdownSections.parse("""
                # One

                body

                # Two
                """);

        assertThat(sections.getFirst().startLine()).isEqualTo(1);
        assertThat(sections.getFirst().endLine()).isEqualTo(4);
    }

    @Test
    void hasNoTitleWhenThereIsNoHeading() {
        assertThat(MarkdownSections.title(MarkdownSections.parse("just prose\n"))).isNull();
    }
}
