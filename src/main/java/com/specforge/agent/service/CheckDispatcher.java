package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.RunState;
import com.specforge.agent.event.ChecksQueued;
import com.specforge.agent.repository.CheckRunRepository;
import com.specforge.catalog.SpecCatalog;
import com.specforge.platform.api.Problems;
import com.specforge.repository.ProjectRef;
import com.specforge.repository.Projects;
import com.specforge.review.ReviewComparison;
import com.specforge.review.Reviews;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Queues runs, and retires the ones the review has moved past.
 *
 * <p>Nothing is deleted here. A superseded run is marked stale, and one that had not started yet is
 * finished as skipped with the reason — so "what did the previous head say" stays answerable, and a
 * queued run for a head nobody is reviewing any more never costs a model call.
 */
@RequiredArgsConstructor
@Service
@Transactional
class CheckDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CheckDispatcher.class);
    private static final String SUPERSEDED = "Superseded by a newer head.";

    private final CheckRunRepository runs;
    private final CheckCatalogue catalogue;
    private final Reviews reviews;
    private final SpecCatalog catalog;
    private final Projects projects;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /** A fresh set of runs for every enabled check, against the review's current head. */
    List<CheckRunEntity> dispatchAll(final UUID reviewId) {
        final ReviewComparison comparison = reviews.comparison(reviewId).orElse(null);
        if (comparison == null) {
            // A review whose base version is gone has nothing to compare against; the import that
            // recreates it will set the head again and dispatch then.
            log.warn("Review {} has no comparable base, so no checks were dispatched", reviewId);
            return List.of();
        }
        final UUID projectId = projectId(comparison.documentId());
        if (projectId == null) {
            log.warn("Review {} has no project behind it, so no checks were dispatched", reviewId);
            return List.of();
        }

        final Instant now = clock.instant();
        retire(reviewId, now);
        final List<CheckRunEntity> queued = new ArrayList<>();
        for (final CheckDefinitionEntity definition : catalogue.enabledForProject(projectId)) {
            queued.add(queue(definition, comparison, now));
        }
        publish(reviewId, queued);
        return List.copyOf(queued);
    }

    /** One check again, on the current head, with its previous run kept and marked stale. */
    List<CheckRunEntity> dispatchOne(final UUID checkRunId) {
        final CheckRunEntity previous = runs.findById(checkRunId)
                .orElseThrow(() -> Problems.notFound("No check run %s.".formatted(checkRunId)));
        final ReviewComparison comparison = reviews.comparison(previous.reviewId())
                .orElseThrow(() -> Problems.conflict("Review %s has no version to check.".formatted(previous.reviewId())));
        final CheckDefinitionEntity definition = catalogue.definition(previous.checkDefinitionId())
                .orElseThrow(() -> Problems.conflict("The check behind run %s is no longer configured.".formatted(checkRunId)));

        final Instant now = clock.instant();
        if (!previous.state().terminal()) {
            previous.finish(RunState.SKIPPED, "Replaced by a rerun before it finished.", null, now);
        }
        previous.markStale(now);
        runs.save(previous);

        final List<CheckRunEntity> queued = List.of(queue(definition, comparison, now));
        publish(previous.reviewId(), queued);
        return queued;
    }

    /**
     * Retires every live run of the review: a queued or running one is cancelled rather than
     * executed, which is the debounce — a further head advance must not pay for the head before it.
     */
    private void retire(final UUID reviewId, final Instant now) {
        for (final CheckRunEntity run : runs.findByReviewIdOrderByQueuedAtAsc(reviewId)) {
            if (run.stale()) {
                continue;
            }
            if (!run.state().terminal()) {
                run.finish(RunState.SKIPPED, SUPERSEDED, null, now);
            }
            run.markStale(now);
            runs.save(run);
        }
    }

    private CheckRunEntity queue(
            final CheckDefinitionEntity definition, final ReviewComparison comparison, final Instant now) {
        return runs.save(new CheckRunEntity(
                UUID.randomUUID(),
                comparison.reviewId(),
                definition.id(),
                comparison.headContentSha(),
                comparison.headLabel(),
                now));
    }

    private void publish(final UUID reviewId, final List<CheckRunEntity> queued) {
        if (!queued.isEmpty()) {
            events.publishEvent(new ChecksQueued(reviewId, queued.stream().map(CheckRunEntity::id).toList()));
        }
    }

    private UUID projectId(final UUID documentId) {
        return catalog.locate(documentId)
                .flatMap(location -> projects.forConnection(location.connectionId()))
                .map(ProjectRef::projectId)
                .orElse(null);
    }
}
