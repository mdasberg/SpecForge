package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.DispositionKind;
import com.specforge.agent.entity.FindingDispositionEntity;
import com.specforge.agent.entity.FindingEntity;
import com.specforge.agent.entity.RunState;
import com.specforge.agent.entity.RunnerKind;
import com.specforge.agent.repository.CheckRunRepository;
import com.specforge.agent.repository.FindingDispositionRepository;
import com.specforge.agent.repository.FindingRepository;
import com.specforge.review.ReviewComparison;
import com.specforge.review.Reviews;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one queued check and records what it concluded.
 *
 * <p>The head is re-read here rather than trusted from the queued row: between queueing and running
 * a pull request can be pushed to again, and a result attributed to a head nobody is reviewing is
 * worse than no result. That check is the debounce the design asks for, and it is deliberately at
 * the last possible moment.
 */
@Service
@Transactional
class CheckExecution {

    private static final Logger log = LoggerFactory.getLogger(CheckExecution.class);

    private final CheckRunRepository runs;
    private final FindingRepository findings;
    private final FindingDispositionRepository dispositions;
    private final CheckCatalogue catalogue;
    private final Reviews reviews;
    private final Clock clock;
    private final Map<RunnerKind, CheckRunner> runners = new EnumMap<>(RunnerKind.class);

    CheckExecution(
            final CheckRunRepository runs,
            final FindingRepository findings,
            final FindingDispositionRepository dispositions,
            final CheckCatalogue catalogue,
            final Reviews reviews,
            final Clock clock,
            final List<CheckRunner> registered) {
        this.runs = runs;
        this.findings = findings;
        this.dispositions = dispositions;
        this.catalogue = catalogue;
        this.reviews = reviews;
        this.clock = clock;
        registered.forEach(runner -> runners.put(runner.kind(), runner));
    }

    void execute(final UUID runId) {
        final CheckRunEntity run = runs.findById(runId).orElse(null);
        if (run == null || run.state() != RunState.QUEUED || run.stale()) {
            return;
        }
        final CheckDefinitionEntity definition = catalogue.definition(run.checkDefinitionId()).orElse(null);
        if (definition == null) {
            finish(run, new CheckOutcome(RunState.SKIPPED, "This check is no longer configured.", null, List.of()));
            return;
        }
        final ReviewComparison comparison = reviews.comparison(run.reviewId()).orElse(null);
        if (comparison == null || !comparison.headContentSha().equals(run.headContentSha())) {
            final Instant now = clock.instant();
            run.finish(RunState.SKIPPED, "Superseded by a newer head before it ran.", null, now);
            run.markStale(now);
            runs.save(run);
            return;
        }
        final CheckRunner runner = runners.get(definition.runnerKind());
        if (runner == null) {
            finish(run, new CheckOutcome(
                    RunState.SKIPPED, "No runner is registered for %s checks.".formatted(definition.runnerKind()),
                    null, List.of()));
            return;
        }

        run.start(clock.instant());
        runs.save(run);
        CheckOutcome outcome;
        try {
            outcome = runner.run(new CheckContext(
                    definition.checkKey(), definition.configurationLines(), comparison));
        } catch (final RuntimeException e) {
            // A runner that threw has not found anything; reporting a failure without findings says
            // exactly that, and keeps one broken check from stopping the other seven.
            log.warn("Check {} on review {} could not complete", definition.checkKey(), run.reviewId(), e);
            outcome = CheckOutcome.failed("The check could not complete: " + e.getMessage(), List.of());
        }
        record(run, definition, outcome);
        finish(run, outcome);
    }

    private void record(
            final CheckRunEntity run, final CheckDefinitionEntity definition, final CheckOutcome outcome) {
        if (outcome.findings().isEmpty()) {
            return;
        }
        final String author = AgentIdentities.authorOf(definition.runnerKind());
        // Read before anything is written, or the new findings would match themselves.
        final Map<String, FindingDispositionEntity> carried = disposedBefore(run.reviewId());
        final Instant now = clock.instant();
        for (final RunnerFinding finding : outcome.findings()) {
            final FindingEntity saved = findings.save(new FindingEntity(
                    UUID.randomUUID(),
                    run.id(),
                    run.reviewId(),
                    finding.sectionKey(),
                    finding.rangeStart(),
                    finding.rangeEnd(),
                    finding.severity(),
                    finding.title(),
                    finding.body(),
                    finding.proposedText(),
                    author,
                    now));
            carry(carried.get(key(definition.id(), finding.sectionKey(), finding.title())), saved, now);
        }
    }

    /**
     * A finding the same check already made about the same section, and what a human decided about
     * it. The decision travels with the new head: a reviewer who dismissed something once should not
     * have to dismiss it again on every push, and re-raising it would train them to ignore the tab.
     *
     * <p>The match is deliberately exact — same check, same section key, same title. A section whose
     * key is gone produces no match, which is the same rule the anchors themselves follow: nothing is
     * reattached by resemblance.
     */
    private Map<String, FindingDispositionEntity> disposedBefore(final UUID reviewId) {
        final List<FindingEntity> previous = findings.findByReviewIdOrderByCreatedAtAsc(reviewId);
        if (previous.isEmpty()) {
            return Map.of();
        }
        final Map<UUID, UUID> definitionByRun = new HashMap<>();
        runs.findByReviewIdOrderByQueuedAtAsc(reviewId)
                .forEach(run -> definitionByRun.put(run.id(), run.checkDefinitionId()));
        final Map<UUID, FindingDispositionEntity> latest = new HashMap<>();
        dispositions.findByFindingIdInOrderByCreatedAtAsc(previous.stream().map(FindingEntity::id).toList())
                .forEach(disposition -> latest.put(disposition.findingId(), disposition));

        final Map<String, FindingDispositionEntity> byAnchor = new HashMap<>();
        for (final FindingEntity finding : previous) {
            final FindingDispositionEntity disposition = latest.get(finding.id());
            final UUID definitionId = definitionByRun.get(finding.checkRunId());
            if (disposition != null && definitionId != null) {
                byAnchor.put(key(definitionId, finding.sectionKey(), finding.title()), disposition);
            }
        }
        return byAnchor;
    }

    private void carry(
            final FindingDispositionEntity previous, final FindingEntity finding, final Instant now) {
        if (previous == null || previous.kind() == DispositionKind.UNDONE) {
            return;
        }
        dispositions.save(new FindingDispositionEntity(
                UUID.randomUUID(), finding.id(), previous.kind(), previous.actorSubjectId(),
                previous.threadId(), now));
    }

    private static String key(final UUID definitionId, final String sectionKey, final String title) {
        return definitionId + " " + sectionKey + " " + title;
    }

    private void finish(final CheckRunEntity run, final CheckOutcome outcome) {
        run.finish(outcome.state(), outcome.summary(), outcome.modelIdentity(), clock.instant());
        runs.save(run);
    }
}
