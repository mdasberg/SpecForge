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
import com.specforge.catalog.repository.SpecSectionRepository;
import com.specforge.catalog.repository.SpecVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SpecCatalogService implements SpecCatalog {

    private final SpecDocumentRepository documents;
    private final SpecVersionRepository versions;
    private final SpecSectionRepository sections;
    private final Clock clock;

    SpecCatalogService(
            SpecDocumentRepository documents,
            SpecVersionRepository versions,
            SpecSectionRepository sections,
            Clock clock) {
        this.documents = documents;
        this.versions = versions;
        this.sections = sections;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ImportResult importVersion(SpecImport specImport) {
        String content = SpecContent.normalise(specImport.content());
        List<ParsedSection> parsed = MarkdownSections.parse(content);
        String title = MarkdownSections.title(parsed);
        if (title == null) {
            // Without a heading there is no title and no anchor to hang a discussion on, which is
            // exactly what the scan calls unparsable. Refusing here keeps that judgement in one
            // place instead of letting a titleless document into the catalogue.
            throw new IllegalArgumentException(
                    "%s has no heading, so it cannot be imported as a specification.".formatted(specImport.path()));
        }
        Instant now = clock.instant();
        Set<String> tags = specImport.tags() == null ? Set.of() : specImport.tags();

        SpecDocument document = documents
                .findByConnectionIdAndPath(specImport.connectionId(), specImport.path())
                .orElseGet(() -> documents.save(new SpecDocument(
                        UUID.randomUUID(),
                        specImport.connectionId(),
                        specImport.path(),
                        title,
                        specImport.project(),
                        specImport.domain(),
                        specImport.owningTeam(),
                        specImport.author(),
                        tags,
                        now)));
        document.updateMetadata(title, specImport.domain(), specImport.owningTeam(), specImport.author(), tags, now);

        String contentSha = SpecContent.sha256(content);
        Optional<SpecVersion> current = versions.findFirstByDocumentIdOrderByOrdinalDesc(document.id());
        if (current.isPresent() && current.get().contentSha().equals(contentSha)) {
            return new ImportResult(document.id(), current.get().ordinal(), false);
        }

        int ordinal = current.map(version -> version.ordinal() + 1).orElse(1);
        SpecVersion version = versions.save(new SpecVersion(
                UUID.randomUUID(),
                document.id(),
                ordinal,
                content,
                contentSha,
                specImport.commitSha(),
                specImport.author(),
                now));
        sections.saveAll(sectionsOf(version.id(), parsed));
        return new ImportResult(document.id(), ordinal, true);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SpecLocation> locate(UUID documentId) {
        return documents.findById(documentId).map(document -> new SpecLocation(document.connectionId(), document.path()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> find(UUID connectionId, String path) {
        return documents.findByConnectionIdAndPath(connectionId, path).map(SpecDocument::id);
    }

    @Override
    @Transactional
    public void proposeChange(UUID documentId) {
        SpecDocument document = documents
                .findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("No specification %s.".formatted(documentId)));
        SpecLifecycle.transition(document, SpecStatus.IN_REVIEW, clock.instant());
    }

    /** Ids are assigned before saving so a child can reference the parent that precedes it. */
    private static List<SpecSection> sectionsOf(UUID versionId, List<ParsedSection> parsed) {
        List<UUID> ids = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ids.add(UUID.randomUUID());
        }
        List<SpecSection> rows = new ArrayList<>(parsed.size());
        for (int i = 0; i < parsed.size(); i++) {
            ParsedSection section = parsed.get(i);
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
