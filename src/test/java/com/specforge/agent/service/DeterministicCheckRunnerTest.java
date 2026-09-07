package com.specforge.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.specforge.agent.entity.RunState;
import com.specforge.agent.entity.Severity;
import com.specforge.review.ReviewComparison;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeterministicCheckRunnerTest {

    private static final String COVERED = """
            # Claim preauthorization

            ## Requirements

            ### Requirement: A claim is preauthorized before it is paid
            The system SHALL refuse payment on a claim that has no preauthorization.

            #### Scenario: Unauthorized claim
            - **WHEN** a claim without a preauthorization is submitted for payment
            - **THEN** the payment is refused

            ## API Contract

            ```yaml
            required: [memberId, providerId]
            ```
            """;

    private final DeterministicCheckRunner runner = new DeterministicCheckRunner();

    @Test
    void aRequirementWithAScenarioPasses() {
        final CheckOutcome outcome = run(CheckCatalogue.ACCEPTANCE_CRITERIA, COVERED, COVERED);

        assertThat(outcome.state()).isEqualTo(RunState.PASSED);
        assertThat(outcome.findings()).isEmpty();
    }

    @Test
    void aRequirementWithoutAScenarioFailsWithAnAnchoredFinding() {
        final String uncovered = COVERED.replace("""
                #### Scenario: Unauthorized claim
                - **WHEN** a claim without a preauthorization is submitted for payment
                - **THEN** the payment is refused
                """, "");

        final CheckOutcome outcome = run(CheckCatalogue.ACCEPTANCE_CRITERIA, COVERED, uncovered);

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.sectionKey()).isEqualTo("requirement-a-claim-is-preauthorized-before-it-is-paid-1");
            assertThat(finding.severity()).isEqualTo(Severity.ERROR);
            assertThat(finding.proposedText()).contains("**WHEN**");
        });
    }

    @Test
    void aScenarioMissingItsThenIsAWarningRatherThanAnError() {
        final String half = COVERED.replace("- **THEN** the payment is refused", "- the payment is refused");

        final CheckOutcome outcome = run(CheckCatalogue.ACCEPTANCE_CRITERIA, COVERED, half);

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).singleElement()
                .satisfies(finding -> assertThat(finding.severity()).isEqualTo(Severity.WARNING));
    }

    @Test
    void aRemovedContractLineIsABreakingChange() {
        final String narrowed = COVERED.replace("required: [memberId, providerId]", "required: [memberId]");

        final CheckOutcome outcome = run(CheckCatalogue.API_COMPATIBILITY, COVERED, narrowed);

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.sectionKey()).isEqualTo("api-contract-1");
            assertThat(finding.body()).contains("required: [memberId, providerId]");
        });
    }

    @Test
    void anUnchangedContractIsCompatible() {
        assertThat(run(CheckCatalogue.API_COMPATIBILITY, COVERED, COVERED).state()).isEqualTo(RunState.PASSED);
    }

    @Test
    void aSkippedHeadingLevelIsAStructuralFailure() {
        final String jumped = COVERED.replace("### Requirement: A claim", "##### Requirement: A claim");

        final CheckOutcome outcome = run(CheckCatalogue.OPENSPEC_STRUCTURE, COVERED, jumped);

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).anySatisfy(
                finding -> assertThat(finding.title()).contains("Heading level jumps"));
    }

    @Test
    void aCheckWithNothingConfiguredIsSkippedRatherThanPassed() {
        final CheckOutcome glossary = run(CheckCatalogue.TERMINOLOGY, COVERED, COVERED);
        final CheckOutcome rules = run(CheckCatalogue.ARCHITECTURE_RULES, COVERED, COVERED);

        assertThat(glossary.state()).isEqualTo(RunState.SKIPPED);
        assertThat(rules.state()).isEqualTo(RunState.SKIPPED);
    }

    @Test
    void aGlossaryTermIsReportedWithThePreferredWordAsTheSuggestion() {
        final String wording = COVERED.replace("a claim without a preauthorization", "a claim without a pre-auth");

        final CheckOutcome outcome = runner.run(new CheckContext(
                CheckCatalogue.TERMINOLOGY, List.of("preauthorization | pre-auth | preauth"), comparison(COVERED, wording)));

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).anySatisfy(finding -> {
            assertThat(finding.severity()).isEqualTo(Severity.INFO);
            assertThat(finding.proposedText()).isEqualTo("preauthorization");
        });
    }

    @Test
    void anUnmetArchitectureRuleIsAdvisoryAndNamesTheRule() {
        final CheckOutcome outcome = runner.run(new CheckContext(
                CheckCatalogue.ARCHITECTURE_RULES, List.of("must: idempotency"), comparison(COVERED, COVERED)));

        assertThat(outcome.state()).isEqualTo(RunState.FAILED);
        assertThat(outcome.findings()).singleElement().satisfies(finding -> {
            assertThat(finding.severity()).isEqualTo(Severity.WARNING);
            assertThat(finding.title()).contains("idempotency");
        });
    }

    private CheckOutcome run(final String checkKey, final String base, final String head) {
        return runner.run(new CheckContext(checkKey, List.of(), comparison(base, head)));
    }

    private static ReviewComparison comparison(final String base, final String head) {
        return new ReviewComparison(
                UUID.randomUUID(), UUID.randomUUID(), base, head, "sha-head", "#1", List.of());
    }
}
