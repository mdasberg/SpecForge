package com.specforge.agent.service;

import com.specforge.agent.entity.RunState;
import com.specforge.agent.entity.RunnerKind;
import com.specforge.agent.entity.Severity;
import com.specforge.agent.model.ModelFinding;
import com.specforge.agent.model.ModelRequest;
import com.specforge.agent.model.ModelResult;
import com.specforge.agent.model.ReviewModel;
import com.specforge.catalog.SpecSectionRange;
import com.specforge.catalog.SpecText;
import com.specforge.catalog.SpecTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The checks that are a judgement rather than a parse: security, the edge cases nobody wrote down,
 * and what this change breaks for an existing consumer.
 *
 * <p>They are given the changed sections only. That is partly cost — the unedited half of a
 * specification produces the same findings on every push — and partly signal: a finding on a
 * section this change did not touch is a finding about a decision that was already reviewed.
 */
@RequiredArgsConstructor
@Component
class ModelCheckRunner implements CheckRunner {

    private static final Map<String, String> INSTRUCTIONS = Map.of(
            CheckCatalogue.SECURITY_REVIEW,
            "Look for security consequences the text does not address: unauthenticated access, missing "
                    + "authorisation, data exposed to the wrong tenant or role, secrets in transit or at rest, "
                    + "and audit gaps.",
            CheckCatalogue.EDGE_CASES,
            "Look for cases the text leaves undecided: empty and maximum inputs, concurrent actors, partial "
                    + "failure, retries, and what happens when a referenced thing has been deleted.",
            CheckCatalogue.BREAKING_CHANGES,
            "Look for changes that break an existing consumer of this specification: removed or renamed "
                    + "fields and operations, narrowed types, new required inputs, and changed defaults or "
                    + "error semantics.");

    private final ReviewModel model;

    @Override
    public RunnerKind kind() {
        return RunnerKind.MODEL;
    }

    @Override
    public CheckOutcome run(final CheckContext context) {
        final String instruction = INSTRUCTIONS.get(context.checkKey());
        if (instruction == null) {
            return CheckOutcome.skipped("No instruction is registered for check %s.".formatted(context.checkKey()));
        }
        final SpecText head = SpecTexts.of(context.comparison().headContent());
        final List<SpecSectionRange> changed = head.sections().stream()
                .filter(section -> context.comparison().changedSectionKeys().contains(section.anchorKey()))
                .toList();
        if (changed.isEmpty()) {
            return CheckOutcome.skipped("Nothing in this head differs from the base version.");
        }

        final Optional<ModelResult> result = model.review(
                new ModelRequest(context.checkKey(), instruction, prompt(head, changed)));
        if (result.isEmpty()) {
            // Not a pass: nobody looked. Saying otherwise would let an unconfigured model read as a
            // clean review, which is the one thing a review tool must never claim.
            return CheckOutcome.skipped("No review model is configured, so this check did not run.");
        }

        final Set<String> anchorable = changed.stream().map(SpecSectionRange::anchorKey).collect(Collectors.toSet());
        final List<RunnerFinding> findings = new ArrayList<>();
        for (final ModelFinding finding : result.get().findings()) {
            // A finding whose anchor is not one of the sections the model was shown cannot be
            // placed in the document, and a finding a reviewer cannot see in context is noise.
            if (anchorable.contains(finding.sectionKey())) {
                findings.add(new RunnerFinding(
                        finding.sectionKey(),
                        null,
                        null,
                        severity(finding.severity()),
                        finding.title(),
                        finding.body(),
                        finding.proposedText()));
            }
        }
        final String modelIdentity = result.get().modelIdentity();
        if (findings.isEmpty()) {
            return CheckOutcome.passed("%d changed section%s reviewed, nothing found.".formatted(
                    changed.size(), changed.size() == 1 ? "" : "s")).from(modelIdentity);
        }
        return new CheckOutcome(
                RunState.FAILED,
                "%d observation%s across %d changed section%s.".formatted(
                        findings.size(), findings.size() == 1 ? "" : "s",
                        changed.size(), changed.size() == 1 ? "" : "s"),
                modelIdentity,
                List.copyOf(findings));
    }

    /** The changed sections, each under its anchor key, which is what a finding has to cite. */
    private static String prompt(final SpecText head, final List<SpecSectionRange> changed) {
        final StringBuilder prompt = new StringBuilder();
        for (final SpecSectionRange section : changed) {
            prompt.append("SECTION ").append(section.anchorKey()).append('\n')
                    .append(String.join("\n", SpecSections.lines(head, section)))
                    .append("\n\n");
        }
        return prompt.toString().strip();
    }

    private static Severity severity(final String claimed) {
        if (claimed == null) {
            return Severity.INFO;
        }
        return switch (claimed.toUpperCase(Locale.ROOT)) {
            case "ERROR", "CRITICAL", "HIGH" -> Severity.ERROR;
            case "WARNING", "WARN", "MEDIUM", "MAJOR" -> Severity.WARNING;
            default -> Severity.INFO;
        };
    }
}
