package com.specforge.review.service;

import com.specforge.catalog.SpecTexts;
import com.specforge.review.AnchorState;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AnchorCarryTest {

    private static final String HEAD_V2 = """
            # Claim preauthorization

            ## Purpose

            Preauthorization decides whether a claim may proceed.

            ## Validation Rules

            The member must be active.

            ## API Contract

            POST /preauthorizations
            """;

    @Test
    void anUntouchedSectionKeepsItsAnchors() {
        final String v3 = HEAD_V2.replace("POST /preauthorizations", "POST /v2/preauthorizations");

        final Map<String, AnchorState> states = carry(HEAD_V2, v3);

        assertThat(states.get("purpose-1")).isEqualTo(AnchorState.CURRENT);
        assertThat(states.get("validation-rules-1")).isEqualTo(AnchorState.CURRENT);
    }

    @Test
    void anEditedSectionMakesItsAnchorsStale() {
        final String v3 = HEAD_V2.replace("The member must be active.", "The member must be enrolled.");

        assertThat(carry(HEAD_V2, v3).get("validation-rules-1")).isEqualTo(AnchorState.STALE);
    }

    @Test
    void aRemovedSectionOrphansItsAnchors() {
        final String v3 = HEAD_V2.replace("""
                ## Validation Rules

                The member must be active.

                """, "");

        assertThat(carry(HEAD_V2, v3).get("validation-rules-1")).isEqualTo(AnchorState.ORPHANED);
    }

    @Test
    void aRenamedHeadingOrphansTheOldKeyAndSaysNothingAboutTheNewOne() {
        final String v3 = HEAD_V2.replace("## Validation Rules", "## Eligibility Rules");

        final Map<String, AnchorState> states = carry(HEAD_V2, v3);

        assertThat(states.get("validation-rules-1")).isEqualTo(AnchorState.ORPHANED);
        assertThat(states).doesNotContainKey("eligibility-rules-1");
    }

    @Test
    void everyKeyOfThePreviousHeadIsAccountedFor() {
        assertThat(carry(HEAD_V2, HEAD_V2))
                .containsOnlyKeys("claim-preauthorization-1", "purpose-1", "validation-rules-1", "api-contract-1")
                .containsValue(AnchorState.CURRENT);
    }

    private static Map<String, AnchorState> carry(final String previousHead, final String newHead) {
        return AnchorCarry.states(SpecTexts.of(previousHead), SpecTexts.of(newHead));
    }
}
