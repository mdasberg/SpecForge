package com.specforge.agent.service;

import com.specforge.agent.entity.RunnerKind;
import com.specforge.agent.entity.Severity;
import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The checks that are a parser rather than an opinion: structure, acceptance-criteria coverage,
 * contract compatibility, the project's architecture rules and its glossary.
 *
 * <p>They run on every head, unlike the model-backed ones, because they are cheap and their answer
 * is the same every time — which is also what makes them the ones worth blocking an approval on.
 */
@Component
class DeterministicCheckRunner implements CheckRunner {

    @Override
    public RunnerKind kind() {
        return RunnerKind.DETERMINISTIC;
    }

    @Override
    public CheckOutcome run(final CheckContext context) {
        final SpecText head = SpecTexts.of(context.comparison().headContent());
        return switch (context.checkKey()) {
            case CheckCatalogue.OPENSPEC_STRUCTURE -> structure(head);
            case CheckCatalogue.ACCEPTANCE_CRITERIA -> acceptanceCriteria(head);
            case CheckCatalogue.API_COMPATIBILITY -> apiCompatibility(context, head);
            case CheckCatalogue.ARCHITECTURE_RULES -> architectureRules(context, head);
            case CheckCatalogue.TERMINOLOGY -> terminology(context, head);
            default -> CheckOutcome.skipped("No runner is registered for check %s.".formatted(context.checkKey()));
        };
    }

    /** Headings that step down more than one level, and requirements that say nothing. */
    private static CheckOutcome structure(final SpecText head) {
        final List<RunnerFinding> findings = new ArrayList<>();
        int previousLevel = 0;
        for (final SpecSectionRange section : head.sections()) {
            if (previousLevel > 0 && section.level() > previousLevel + 1) {
                findings.add(RunnerFinding.on(
                        section.anchorKey(),
                        Severity.ERROR,
                        "Heading level jumps from %d to %d".formatted(previousLevel, section.level()),
                        ("“%s” is nested %d levels deep under a level %d heading. A skipped level leaves the "
                                + "section without a parent, and the outline it produces is not the document's "
                                + "structure.").formatted(SpecSections.title(section), section.level(), previousLevel)));
            }
            previousLevel = section.level();
            if (SpecSections.isRequirement(section) && SpecSections.body(head, section).stream().allMatch(String::isBlank)) {
                findings.add(RunnerFinding.on(
                        section.anchorKey(),
                        Severity.ERROR,
                        "Requirement has no text",
                        "“%s” states no behaviour, so there is nothing to approve or to build."
                                .formatted(SpecSections.title(section))));
            }
        }
        return findings.isEmpty()
                ? CheckOutcome.passed("%d headings, all well-formed.".formatted(head.sections().size()))
                : CheckOutcome.failed("%d structural problem%s.".formatted(
                        findings.size(), findings.size() == 1 ? "" : "s"), findings);
    }

    /** Every requirement needs at least one scenario, and a scenario needs a WHEN and a THEN. */
    private static CheckOutcome acceptanceCriteria(final SpecText head) {
        final List<RunnerFinding> findings = new ArrayList<>();
        int requirements = 0;
        for (final SpecSectionRange section : head.sections()) {
            if (!SpecSections.isRequirement(section)) {
                continue;
            }
            requirements++;
            final List<SpecSectionRange> scenarios = SpecSections.nested(head, section).stream()
                    .filter(SpecSections::isScenario)
                    .toList();
            if (scenarios.isEmpty()) {
                findings.add(new RunnerFinding(
                        section.anchorKey(),
                        null,
                        null,
                        Severity.ERROR,
                        "No scenario covers this requirement",
                        ("“%s” has no scenario, so nothing states how anyone would know it was met.")
                                .formatted(SpecSections.title(section)),
                        "#### Scenario: <the case this requirement is about>\n- **WHEN** …\n- **THEN** …"));
                continue;
            }
            for (final SpecSectionRange scenario : scenarios) {
                final String body = String.join("\n", SpecSections.body(head, scenario)).toUpperCase(Locale.ROOT);
                if (!body.contains("**WHEN**") || !body.contains("**THEN**")) {
                    findings.add(RunnerFinding.on(
                            scenario.anchorKey(),
                            Severity.WARNING,
                            "Scenario is missing a WHEN or a THEN",
                            ("“%s” does not state both the trigger and the expected outcome, which is what makes "
                                    + "a scenario testable.").formatted(SpecSections.title(scenario))));
                }
            }
        }
        if (requirements == 0) {
            return CheckOutcome.skipped("This specification states no requirements.");
        }
        return findings.isEmpty()
                ? CheckOutcome.passed("%d requirement%s, all covered by scenarios.".formatted(
                        requirements, requirements == 1 ? "" : "s"))
                : CheckOutcome.failed("%d of %d requirements are not properly covered.".formatted(
                        findings.size(), requirements), findings);
    }

    /** Contract lines the base declared and the head no longer does — what breaks a consumer. */
    private static CheckOutcome apiCompatibility(final CheckContext context, final SpecText head) {
        final Map<String, List<String>> baseBlocks =
                ContractBlocks.bySection(SpecTexts.of(context.comparison().baseContent()));
        if (baseBlocks.isEmpty()) {
            return CheckOutcome.skipped("This specification declares no API contract.");
        }
        final Map<String, List<String>> headBlocks = ContractBlocks.bySection(head);
        final List<RunnerFinding> findings = new ArrayList<>();
        for (final Map.Entry<String, List<String>> base : baseBlocks.entrySet()) {
            final List<String> removed = ContractBlocks.removed(
                    base.getValue(), headBlocks.getOrDefault(base.getKey(), List.of()));
            if (!removed.isEmpty()) {
                findings.add(RunnerFinding.on(
                        base.getKey(),
                        Severity.ERROR,
                        "%d contract line%s removed".formatted(removed.size(), removed.size() == 1 ? "" : "s"),
                        ("The base version declared this and the head no longer does, so anything relying on it "
                                + "breaks:\n\n```\n%s\n```").formatted(String.join("\n", removed))));
            }
        }
        return findings.isEmpty()
                ? CheckOutcome.passed("No declared contract shrank against the base version.")
                : CheckOutcome.failed("%d contract section%s changed incompatibly.".formatted(
                        findings.size(), findings.size() == 1 ? "" : "s"), findings);
    }

    /**
     * The project's architecture rules, each configured as {@code must: <phrase>} or
     * {@code must-not: <phrase>}. Advisory: a rule list is a team's habit, not a law, and blocking
     * on a phrase match would make the check a thing to work around.
     */
    private static CheckOutcome architectureRules(final CheckContext context, final SpecText head) {
        if (context.configuration().isEmpty()) {
            return CheckOutcome.skipped("No architecture rules are configured for this project.");
        }
        if (head.sections().isEmpty()) {
            // Every finding anchors to a section, so a document with no headings has nowhere to
            // report a document-level rule against.
            return CheckOutcome.skipped("This specification has no sections to anchor a rule to.");
        }
        final String content = head.content().toLowerCase(Locale.ROOT);
        final List<RunnerFinding> findings = new ArrayList<>();
        final String anchor = firstAnchor(head);
        for (final String rule : context.configuration()) {
            final boolean forbidden = rule.toLowerCase(Locale.ROOT).startsWith("must-not:");
            final String phrase = rule.substring(rule.indexOf(':') + 1).trim().toLowerCase(Locale.ROOT);
            if (phrase.isEmpty()) {
                continue;
            }
            final boolean present = content.contains(phrase);
            if (forbidden && present) {
                findings.add(RunnerFinding.on(anchor, Severity.WARNING, "Rule broken: %s".formatted(rule),
                        "This project's rules say the specification must not mention “%s”.".formatted(phrase)));
            } else if (!forbidden && !present) {
                findings.add(RunnerFinding.on(anchor, Severity.WARNING, "Rule unmet: %s".formatted(rule),
                        "This project's rules expect the specification to address “%s”.".formatted(phrase)));
            }
        }
        return findings.isEmpty()
                ? CheckOutcome.passed("%d rule%s satisfied.".formatted(
                        context.configuration().size(), context.configuration().size() == 1 ? "" : "s"))
                : CheckOutcome.failed("%d rule%s not satisfied.".formatted(
                        findings.size(), findings.size() == 1 ? "" : "s"), findings);
    }

    /**
     * The project's glossary, each line {@code preferred | alternative | alternative}. A section
     * using an alternative gets one finding naming the preferred term, with it as the suggestion.
     */
    private static CheckOutcome terminology(final CheckContext context, final SpecText head) {
        if (context.configuration().isEmpty()) {
            return CheckOutcome.skipped("No glossary is configured for this project.");
        }
        final List<RunnerFinding> findings = new ArrayList<>();
        for (final SpecSectionRange section : head.sections()) {
            final String body = String.join("\n", SpecSections.body(head, section)).toLowerCase(Locale.ROOT);
            for (final String entry : context.configuration()) {
                final String[] terms = entry.split("\\|");
                if (terms.length < 2) {
                    continue;
                }
                final String preferred = terms[0].trim();
                for (int i = 1; i < terms.length; i++) {
                    final String alternative = terms[i].trim().toLowerCase(Locale.ROOT);
                    if (!alternative.isEmpty() && body.contains(alternative)) {
                        findings.add(new RunnerFinding(
                                section.anchorKey(), null, null, Severity.INFO,
                                "“%s” is the project's term, not “%s”".formatted(preferred, terms[i].trim()),
                                ("The glossary prefers “%s”. One word for one thing is what keeps a specification "
                                        + "readable across teams.").formatted(preferred),
                                preferred));
                    }
                }
            }
        }
        return findings.isEmpty()
                ? CheckOutcome.passed("Terminology matches the project glossary.")
                : CheckOutcome.failed("%d wording%s differ from the glossary.".formatted(
                        findings.size(), findings.size() == 1 ? "" : "s"), findings);
    }

    /** A document-level finding still has to anchor somewhere; the first heading is where it shows. */
    private static String firstAnchor(final SpecText head) {
        return head.sections().isEmpty() ? "" : head.sections().get(0).anchorKey();
    }
}
