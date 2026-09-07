package com.specforge.agent.service;

import com.specforge.agent.entity.CheckDefinitionEntity;
import com.specforge.agent.entity.RunnerKind;
import com.specforge.agent.repository.CheckDefinitionRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The set of checks a project runs, and the initial set it starts with.
 *
 * <p>The definitions are rows rather than constants because blocking, enabled and the check's own
 * configuration are a team's decisions and have to be editable and auditable. The constants below
 * are only what a project is given on first sight; nothing reads them again afterwards, so an
 * administrator's edit is never quietly reverted by a redeploy.
 */
@RequiredArgsConstructor
@Service
@Transactional
class CheckCatalogue {

    static final String OPENSPEC_STRUCTURE = "openspec-structure";
    static final String ACCEPTANCE_CRITERIA = "acceptance-criteria";
    static final String API_COMPATIBILITY = "api-compatibility";
    static final String ARCHITECTURE_RULES = "architecture-rules";
    static final String TERMINOLOGY = "terminology";
    static final String SECURITY_REVIEW = "security-review";
    static final String EDGE_CASES = "edge-cases";
    static final String BREAKING_CHANGES = "breaking-changes";

    /**
     * The eight checks a project starts with. The deterministic ones block because their answer is
     * the same every time and a reviewer can act on it; the model-backed ones are advisory because
     * a non-deterministic result is not something to hold a release against.
     */
    private static final List<Seed> INITIAL = List.of(
            new Seed(OPENSPEC_STRUCTURE, "OpenSpec structure", true, RunnerKind.DETERMINISTIC),
            new Seed(ACCEPTANCE_CRITERIA, "Acceptance criteria coverage", true, RunnerKind.DETERMINISTIC),
            new Seed(API_COMPATIBILITY, "API compatibility", true, RunnerKind.DETERMINISTIC),
            new Seed(ARCHITECTURE_RULES, "Architecture rules", false, RunnerKind.DETERMINISTIC),
            new Seed(TERMINOLOGY, "Terminology consistency", false, RunnerKind.DETERMINISTIC),
            new Seed(SECURITY_REVIEW, "Security review", false, RunnerKind.MODEL),
            new Seed(EDGE_CASES, "Missing edge cases", false, RunnerKind.MODEL),
            new Seed(BREAKING_CHANGES, "Breaking change analysis", false, RunnerKind.MODEL));

    private final CheckDefinitionRepository definitions;
    private final Clock clock;

    /** Every definition the project has, seeding the initial set the first time it is asked for. */
    List<CheckDefinitionEntity> forProject(final UUID projectId) {
        final List<CheckDefinitionEntity> existing = definitions.findByProjectIdOrderByDisplayNameAsc(projectId);
        if (!existing.isEmpty()) {
            return existing;
        }
        for (final Seed seed : INITIAL) {
            definitions.save(new CheckDefinitionEntity(
                    UUID.randomUUID(), projectId, seed.key(), seed.displayName(), seed.blocking(), seed.kind(),
                    true, clock.instant()));
        }
        return definitions.findByProjectIdOrderByDisplayNameAsc(projectId);
    }

    /** The definitions worth dispatching: a disabled check is not work that was skipped. */
    List<CheckDefinitionEntity> enabledForProject(final UUID projectId) {
        forProject(projectId);
        return definitions.findByProjectIdAndEnabledTrueOrderByDisplayNameAsc(projectId);
    }

    /** The definition behind a run, which is where its display name and blocking flag are read live. */
    Optional<CheckDefinitionEntity> definition(final UUID definitionId) {
        return definitions.findById(definitionId);
    }

    /** Definitions by id, for reading a page of runs without a query per run. */
    Map<UUID, CheckDefinitionEntity> definitions(final Collection<UUID> definitionIds) {
        return definitions.findAllById(definitionIds).stream()
                .collect(Collectors.toMap(CheckDefinitionEntity::id, definition -> definition));
    }

    private record Seed(String key, String displayName, boolean blocking, RunnerKind kind) {}
}
