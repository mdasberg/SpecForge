package com.specforge.catalog.service;

import com.specforge.catalog.ImportResult;
import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecImport;
import com.specforge.catalog.SpecLocation;
import com.specforge.catalog.SpecStatus;
import com.specforge.catalog.entity.SpecDocument;
import com.specforge.catalog.entity.SpecSection;
import com.specforge.catalog.entity.SpecVersion;
import com.specforge.catalog.repository.SpecDocumentRepository;
import com.specforge.catalog.repository.SpecSearchRepository;
import com.specforge.catalog.repository.SpecSectionRepository;
import com.specforge.catalog.repository.SpecVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class SpecCatalogService implements SpecCatalog {

    private final SpecDocumentRepository documents;
    private final SpecVersionRepository versions;
    private final SpecSectionRepository sections;
    private final SpecSearchRepository search;
    private final Clock clock;

    @Override
    @Transactional
    public ImportResult importVersion(final SpecImport specImport) {
        final String content = SpecContent.normalise(specImport.content());
        final List<ParsedSection> parsed = MarkdownSections.parse(content);
        final String title = MarkdownSections.title(parsed);
        if (title == null) {
            // Without a heading there is no title and no anchor to hang a discussion on, which is
            // exactly what the scan calls unparsable. Refusing here keeps that judgement in one
            // place instead of letting a titleless document into the catalogue.
            throw new IllegalArgumentException(
                    "%s has no heading, so it cannot be imported as a specification.".formatted(specImport.path()));
        }
        final Instant now = clock.instant();
        final Set<String> tags = specImport.tags() == null ? Set.of() : specImport.tags();

        final SpecDocument document = documents
                .findByConnectionIdAndPath(specImport.connectionId(), specImport.path())
                .orElseGet(() -> documents.save(new SpecDocument(
                        UUID.randomUUID(),
                        specImport.connectionId(),
                        specImport.repositoryFullName(),
                        specImport.path(),
                        title,
                        specImport.project(),
                        specImport.domain(),
                        specImport.owningTeam(),
                        specImport.author(),
                        tags,
                        now)));
        document.updateMetadata(specImport.repositoryFullName(), title, specImport.domain(),
                specImport.owningTeam(), specImport.author(), tags, now);

        final String contentSha = SpecContent.sha256(content);
        final Optional<SpecVersion> current = versions.findFirstByDocumentIdOrderByOrdinalDesc(document.id());
        if (current.isPresent() && current.get().contentSha().equals(contentSha)) {
            return new ImportResult(document.id(), current.get().ordinal(), false);
        }

        final int ordinal = current.map(version -> version.ordinal() + 1).orElse(1);
        final SpecVersion version = versions.save(new SpecVersion(
                UUID.randomUUID(),
                document.id(),
                ordinal,
                content,
                contentSha,
                specImport.commitSha(),
                specImport.author(),
                now));
        sections.saveAll(sectionsOf(version.id(), parsed));
        // Only the current version is searchable, so the previous version's rows go before the new
        // ones arrive; a hit on text that has since been rewritten would point at nothing.
        search.deleteByDocumentId(document.id());
        search.saveAll(SpecSearchIndex.rowsOf(
                document.id(), version.id(), title, document.path(), content, parsed));
        return new ImportResult(document.id(), ordinal, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpecLocation> locate(final UUID documentId) {
        return documents.findById(documentId).map(document -> new SpecLocation(document.connectionId(),
                document.path()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> find(final UUID connectionId, final String path) {
        return documents.findByConnectionIdAndPath(connectionId, path).map(SpecDocument::id);
    }

    @Override
    @Transactional
    public void proposeChange(final UUID documentId) {
        final SpecDocument document = documents
                .findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No specification %s.".formatted(documentId)));
        SpecLifecycle.transition(document, SpecStatus.IN_REVIEW, clock.instant());
    }

    /** Ids are assigned before saving so a child can reference the parent that precedes it. */
    private static List<SpecSection> sectionsOf(final UUID versionId, final List<ParsedSection> parsed) {
        final List<UUID> ids = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ids.add(UUID.randomUUID());
        }
        final List<SpecSection> rows = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            final ParsedSection section = parsed.get(i);
            rows.add(new SpecSection(
                    ids.get(i),
                    versionId,
                    section.parentIndex() < 0 ? null : ids.get(section.parentIndex()),
                    section.anchorKey(),
                    section.heading(),
                    section.level(),
                    i + 1,
                    section.startLine(),
                    section.endLine()));
        }
        return rows;
    }
}
