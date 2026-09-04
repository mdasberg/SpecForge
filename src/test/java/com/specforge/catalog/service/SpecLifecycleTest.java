package com.specforge.catalog.service;

import com.specforge.catalog.SpecStatus;
import com.specforge.catalog.entity.SpecDocument;
import com.specforge.catalog.exception.IllegalSpecTransitionException;
import com.specforge.catalog.service.SpecLifecycle;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;



class SpecLifecycleTest {

    private static final Instant NOW = Instant.parse("2026-09-03T10:00:00Z");

    @Test
    void movesADraftIntoReviewWhenAChangeIsProposed() {
        SpecDocument document = document();

        SpecLifecycle.transition(document, SpecStatus.IN_REVIEW, NOW);

        assertThat(document.status()).isEqualTo(SpecStatus.IN_REVIEW);
    }

    @Test
    void refusesADraftGoingStraightToApproved() {
        SpecDocument document = document();

        assertThatThrownBy(() -> SpecLifecycle.transition(document, SpecStatus.APPROVED, NOW))
                .isInstanceOf(IllegalSpecTransitionException.class);
        assertThat(document.status()).isEqualTo(SpecStatus.DRAFT);
    }

    @Test
    void letsAnApprovedSpecificationReenterReview() {
        SpecDocument document = document();
        SpecLifecycle.transition(document, SpecStatus.IN_REVIEW, NOW);
        SpecLifecycle.transition(document, SpecStatus.APPROVED, NOW);

        SpecLifecycle.transition(document, SpecStatus.IN_REVIEW, NOW);

        assertThat(document.status()).isEqualTo(SpecStatus.IN_REVIEW);
    }

    @Test
    void treatsATransitionToTheCurrentStatusAsNothingToDo() {
        SpecDocument document = document();

        SpecLifecycle.transition(document, SpecStatus.DRAFT, NOW);

        assertThat(document.status()).isEqualTo(SpecStatus.DRAFT);
    }

    @Test
    void allowsOnlyTheDocumentedTransitions() {
        assertThat(SpecLifecycle.isLegal(SpecStatus.IN_REVIEW, SpecStatus.CHANGES_REQUESTED)).isTrue();
        assertThat(SpecLifecycle.isLegal(SpecStatus.CHANGES_REQUESTED, SpecStatus.IN_REVIEW)).isTrue();
        assertThat(SpecLifecycle.isLegal(SpecStatus.APPROVED, SpecStatus.IMPLEMENTED)).isTrue();
        assertThat(SpecLifecycle.isLegal(SpecStatus.IN_REVIEW, SpecStatus.IMPLEMENTED)).isFalse();
        assertThat(SpecLifecycle.isLegal(SpecStatus.DRAFT, SpecStatus.CHANGES_REQUESTED)).isFalse();
    }

    private static SpecDocument document() {
        return new SpecDocument(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "acme/care-management-specs",
                "openspec/specs/billing/spec.md",
                "Billing",
                "Care Management",
                "billing",
                "Payments",
                "ada",
                Set.of(),
                NOW);
    }
}
