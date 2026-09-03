package com.specforge.repository.internal;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
import com.specforge.repository.internal.forge.Forge;
import com.specforge.repository.internal.forge.ForgeFile;
import com.specforge.repository.internal.forge.ForgeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns matched files into specification documents. Every file gets a row in the import run,
 * including the ones nothing happened to, because "why is this file not a specification" has to be
 * answerable afterwards without running the import again.
 */
@Component
class SpecImporter {

    private static final Logger log = LoggerFactory.getLogger(SpecImporter.class);

    private final Forge forge;
    private final SpecCatalog catalog;
    private final ImportRunFileRepository runFiles;

    SpecImporter(Forge forge, SpecCatalog catalog, ImportRunFileRepository runFiles) {
        this.forge = forge;
        this.catalog = catalog;
        this.runFiles = runFiles;
    }

    ImportSummary importPaths(
            RepositoryConnectionEntity connection,
            ProjectEntity project,
            String installationExternalId,
            ForgeRef ref,
            List<String> paths,
            UUID runId) {
        List<ImportRunFileEntity> outcomes = new ArrayList<>(paths.size());
        int imported = 0;
        int unchanged = 0;
        int skipped = 0;
        int failed = 0;

        for (String path : paths) {
            FileOutcome outcome;
            String reason = null;
            try {
                Optional<ForgeFile> file = forge.readFile(installationExternalId, ref, path);
                if (file.isEmpty()) {
                    outcome = FileOutcome.SKIPPED;
                    reason = "The file could not be read from the repository.";
                } else {
                    SpecClassifier.Verdict verdict = SpecClassifier.classify(path, file.get().content());
                    switch (verdict.classification()) {
                        case IMPORTABLE_SPEC -> {
                            boolean created = importOne(connection, project, path, file.get());
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
            } catch (RuntimeException e) {
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
            RepositoryConnectionEntity connection, ProjectEntity project, String path, ForgeFile file) {
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
