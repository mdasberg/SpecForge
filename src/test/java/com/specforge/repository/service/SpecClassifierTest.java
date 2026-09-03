package com.specforge.repository.service;

import com.specforge.repository.entity.Classification;
import com.specforge.repository.service.SpecClassifier;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



class SpecClassifierTest {

    @Test
    void callsAFileWithAHeadingImportable() {
        assertThat(SpecClassifier.classify("openspec/specs/billing/spec.md", "# Billing\n\nText.\n").classification())
                .isEqualTo(Classification.IMPORTABLE_SPEC);
    }

    @Test
    void callsAFileUnderChangesAChangeProposal() {
        assertThat(SpecClassifier.classify("openspec/changes/add-billing/specs/billing/spec.md", "# Billing\n")
                        .classification())
                .isEqualTo(Classification.CHANGE_PROPOSAL);
    }

    @Test
    void explainsWhyAFileWithoutAHeadingCannotBeImported() {
        SpecClassifier.Verdict verdict = SpecClassifier.classify("openspec/specs/x/spec.md", "just prose\n");

        assertThat(verdict.classification()).isEqualTo(Classification.UNPARSABLE);
        assertThat(verdict.reason()).contains("no heading");
    }

    @Test
    void doesNotMistakeAShellCommentForAHeading() {
        assertThat(SpecClassifier.classify("openspec/specs/x/spec.md", "```\n# not a heading\n```\n").classification())
                .isEqualTo(Classification.UNPARSABLE);
    }
}
