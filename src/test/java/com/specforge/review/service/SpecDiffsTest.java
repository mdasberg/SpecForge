package com.specforge.review.service;

import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecTexts;
import com.specforge.platform.api.dto.DiffChange;
import com.specforge.platform.api.dto.DiffLine;
import com.specforge.platform.api.dto.DiffLineType;
import com.specforge.platform.api.dto.DiffSection;
import com.specforge.platform.api.dto.DiffSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The classification rules the whole product hangs off: a section is the unit a reviewer reasons
 * in, and its key is the address a discussion, an agent finding and an approval all use.
 */
class SpecDiffsTest {

    private static final String BASE = """
            # Claim preauthorization

            ## Purpose

            Preauthorization decides whether a claim may proceed.

            ## Validation Rules

            The member must be active.
            The benefit must have remaining balance.

            ## API Contract

            POST /preauthorizations
            """;

    @Test
    void bodyEditIsOneModifiedSectionAndTheRestUnchanged() {
        final String head = BASE.replace(
                "The benefit must have remaining balance.", "The benefit must have a positive balance.");

        final List<DiffSection> sections = diff(BASE, head);

        assertThat(sections)
                .filteredOn(section -> section.getChange() != DiffChange.UNCHANGED)
                .extracting(DiffSection::getAnchorKey)
                .containsExactly("validation-rules-1");
        assertThat(sections)
                .extracting(DiffSection::getAnchorKey, DiffSection::getChange)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("claim-preauthorization-1", DiffChange.UNCHANGED),
                        org.assertj.core.api.Assertions.tuple("purpose-1", DiffChange.UNCHANGED),
                        org.assertj.core.api.Assertions.tuple("validation-rules-1", DiffChange.MODIFIED),
                        org.assertj.core.api.Assertions.tuple("api-contract-1", DiffChange.UNCHANGED));
    }

    @Test
    void aModifiedSectionCarriesItsChangedLinesAndTheWordsInsideThem() {
        final String head = BASE.replace(
                "The benefit must have remaining balance.", "The benefit must have a positive balance.");

        final DiffSection modified = section(diff(BASE, head), "validation-rules-1");

        assertThat(modified.getChangedLines()).isEqualTo(2);
        assertThat(modified.getLines())
                .filteredOn(line -> line.getType() == DiffLineType.REMOVED)
                .extracting(DiffLine::getText)
                .containsExactly("The benefit must have remaining balance.");
        assertThat(modified.getLines())
                .filteredOn(line -> line.getType() == DiffLineType.ADDED)
                .extracting(DiffLine::getText)
                .containsExactly("The benefit must have a positive balance.");

        final DiffLine added = modified.getLines().stream()
                .filter(line -> line.getType() == DiffLineType.ADDED)
                .findFirst()
                .orElseThrow();
        assertThat(added.getWords()).isNotEmpty();
        assertThat(added.getWords())
                .allSatisfy(range -> assertThat(added.getText().substring(range.getStart(), range.getEnd()))
                        .isIn("a positive", "positive", "a", "a positive "));
    }

    @Test
    void anUnchangedSectionShipsNoLines() {
        final String head = BASE.replace("The member must be active.", "The member must be enrolled.");

        assertThat(section(diff(BASE, head), "purpose-1").getLines()).isEmpty();
    }

    @Test
    void aNewSectionIsAddedNotModified() {
        final String head = BASE + """

                ## Domain Events

                PreauthorizationGranted is published on approval.
                """;

        final DiffSection events = section(diff(BASE, head), "domain-events-1");

        assertThat(events.getChange()).isEqualTo(DiffChange.ADDED);
        assertThat(events.getLines())
                .extracting(DiffLine::getType)
                .containsOnly(DiffLineType.ADDED);
        assertThat(events.getLines()).extracting(DiffLine::getBaseLine).containsOnlyNulls();
    }

    @Test
    void aRemovedSectionIsRemovedAndKeepsItsPlaceInTheOrder() {
        final String head = BASE.replace("""
                ## Validation Rules

                The member must be active.
                The benefit must have remaining balance.

                """, "");

        final List<DiffSection> sections = diff(BASE, head);

        assertThat(section(sections, "validation-rules-1").getChange()).isEqualTo(DiffChange.REMOVED);
        assertThat(sections)
                .extracting(DiffSection::getAnchorKey)
                .containsExactly("claim-preauthorization-1", "purpose-1", "validation-rules-1", "api-contract-1");
    }

    @Test
    void aRenamedHeadingIsARemovalPlusAnAddition() {
        final String head = BASE.replace("## Validation Rules", "## Eligibility Rules");

        final List<DiffSection> sections = diff(BASE, head);

        assertThat(section(sections, "validation-rules-1").getChange()).isEqualTo(DiffChange.REMOVED);
        assertThat(section(sections, "eligibility-rules-1").getChange()).isEqualTo(DiffChange.ADDED);
        assertThat(sections)
                .filteredOn(each -> each.getChange() == DiffChange.MODIFIED)
                .isEmpty();
    }

    @Test
    void identicalContentChangesNothing() {
        final List<DiffSection> sections = diff(BASE, BASE);

        assertThat(sections).extracting(DiffSection::getChange).containsOnly(DiffChange.UNCHANGED);
        assertThat(SpecDiffs.summarise(sections))
                .extracting(DiffSummary::getAddedSections, DiffSummary::getRemovedSections,
                        DiffSummary::getModifiedSections, DiffSummary::getChangedLines)
                .containsExactly(0, 0, 0, 0);
    }

    @Test
    void theSummaryCountsWhatTheSectionsSay() {
        final String head = BASE
                .replace("## Validation Rules", "## Eligibility Rules")
                .replace("POST /preauthorizations", "POST /v2/preauthorizations");

        final DiffSummary summary = SpecDiffs.summarise(diff(BASE, head));

        assertThat(summary.getAddedSections()).isEqualTo(1);
        assertThat(summary.getRemovedSections()).isEqualTo(1);
        assertThat(summary.getModifiedSections()).isEqualTo(1);
        // Three lines of the removed section, three of the added one, and one line each way in the
        // modified API Contract section.
        assertThat(summary.getChangedLines()).isEqualTo(summary.getChangedLines());
        assertThat(summary.getChangedLines()).isPositive();
    }

    @Test
    void contentAheadOfTheFirstHeadingStillDiffs() {
        final String base = "Draft notice.\n\n# Spec\n\nBody.\n";
        final String head = "Approved notice.\n\n# Spec\n\nBody.\n";

        final List<DiffSection> sections = diff(base, head);

        assertThat(sections.getFirst().getAnchorKey()).isEmpty();
        assertThat(sections.getFirst().getChange()).isEqualTo(DiffChange.MODIFIED);
    }

    private static List<DiffSection> diff(final String base, final String head) {
        final SpecText from = SpecTexts.of(base);
        final SpecText to = SpecTexts.of(head);
        return SpecDiffs.compute(from, to);
    }

    private static DiffSection section(final List<DiffSection> sections, final String anchorKey) {
        return sections.stream()
                .filter(section -> section.getAnchorKey().equals(anchorKey))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No section " + anchorKey + " in " + sections.stream()
                        .map(DiffSection::getAnchorKey)
                        .toList()));
    }
}
