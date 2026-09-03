package com.specforge.repository.service;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
import com.specforge.repository.entity.FileOutcome;
import com.specforge.repository.entity.ImportRunFileEntity;
import com.specforge.repository.entity.ProjectEntity;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.forge.Forge;
import com.specforge.repository.forge.ForgeFile;
import com.specforge.repository.forge.ForgeRef;
import com.specforge.repository.repository.ImportRunFileRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns matched files into specification documents. Every file gets a row in the import run,
 * including the ones nothing happened to, because "why is this file not a specification" has to be
 * answerable afterwards without running the import again.
 */
@RequiredArgsConstructor
@Component
public class SpecImporter {

    private static final Logger log = LoggerFactory.getLogger(SpecImporter.class);

    private final Forge forge;
    private final SpecCatalog catalog;
    private final ImportRunFileRepository runFiles;

    public ImportSummary importPaths(
            final RepositoryConnectionEntity connection,
            final ProjectEntity project,
            final String installationExternalId,
            final ForgeRef ref,
            final List<String> paths,
            final UUID runId) {
        final List<ImportRunFileEntity> outcomes = new ArrayList<>(paths.size());
        int imported = 0;
        int unchanged = 0;
        int skipped = 0;
        int failed = 0;

        for (final String path : paths) {
            FileOutcome outcome;
            String reason = null;
            try {
                final Optional<ForgeFile> file = forge.readFile(installationExternalId, ref, path);
                if (file.isEmpty()) {
                    outcome = FileOutcome.SKIPPED;
                    reason = "The file could not be read from the repository.";
                } else {
                    final SpecClassifier.Verdict verdict = SpecClassifier.classify(path, file.get().content());
                    switch (verdict.classification()) {
                        case IMPORTABLE_SPEC -> {
                            final boolean created = importOne(connection, project, path, file.get());
                            outcome = created ? FileOutcome.IMPORTED : FileOutcome.UNCHANGED;
                        }
                        case CHANGE_PROPOSAL -> {
                            outcome = FileOutcome.SKIPPED;
                            reason = "The file is a change proposal, not a specification.";
                        }
                        default -> {
                            outcome = FileOutcome.SKIPPED;
                            reason = verdict.reason();
                        }
                    }
                }
            } catch (final RuntimeException e) {
                log.warn("Importing {} from {} failed", path, connection.repositoryFullName(), e);
                outcome = FileOutcome.FAILED;
                reason = e.getMessage();
            }
            switch (outcome) {
                case IMPORTED -> imported++;
                case UNCHANGED -> unchanged++;
                case SKIPPED -> skipped++;
                case FAILED -> failed++;
            }
            outcomes.add(new ImportRunFileEntity(UUID.randomUUID(), runId, path, outcome, reason));
        }
        runFiles.saveAll(outcomes);
        return new ImportSummary(imported, unchanged, skipped, failed);
    }

    private boolean importOne(
            final RepositoryConnectionEntity connection, final ProjectEntity project, final String path, final ForgeFile file) {
        return catalog
                .importVersion(new SpecImport(
                        connection.id(),
                        path,
                        project.name(),
                        SpecPaths.domainOf(path),
                        project.team(),
                        file.author(),
                        FrontMatter.tags(file.content()),
                        file.content(),
                        file.commitSha()))
                .versionCreated();
    }
}
