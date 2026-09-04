package com.specforge.catalog.service;

import com.specforge.catalog.entity.SpecDocument;
import com.specforge.catalog.entity.SpecSection;
import com.specforge.catalog.entity.SpecVersion;
import com.specforge.catalog.repository.SpecSectionRepository;
import com.specforge.catalog.repository.SpecVersionRepository;
import com.specforge.platform.api.Problems;
import com.specforge.platform.api.dto.SpecDetail;
import com.specforge.platform.api.dto.SpecOutlineSection;
import com.specforge.platform.api.dto.SpecStatus;
import com.specforge.platform.api.dto.SpecVersionDocument;
import com.specforge.platform.api.dto.SpecVersionSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Loads a specification document with its requested version and the full version selector list. Opening
 * a specification is one request rather than three, and the version selector needs the complete history
 * to mark which one is current without a second round-trip.
 */
@RequiredArgsConstructor
@Component
class SpecVersionReader {

    private final SpecVersionRepository versions;
    private final SpecSectionRepository sections;

    SpecDetail detail(final SpecDocument document, final Integer requestedOrdinal) {
        final List<SpecVersion> all = versions.findByDocumentIdOrderByOrdinalAsc(document.id());
        if (all.isEmpty()) {
            throw Problems.notFound(
                    "Specification %s has no imported version.".formatted(document.id()));
        }

        final SpecVersion current = all.getLast();
        final SpecVersion selected = requestedOrdinal == null
                ? current
                : all.stream()
                        .filter(version -> version.ordinal() == requestedOrdinal)
                        .findFirst()
                        .orElseThrow(() -> Problems.notFound("Specification %s has no version %d."
                                .formatted(document.id(), requestedOrdinal)));

        final List<SpecSection> sectionList =
                sections.findByVersionIdOrderByOrdinalAsc(selected.id());
        final List<SpecOutlineSection> outline = sectionList.stream()
                .map(section -> new SpecOutlineSection(
                        section.anchorKey(), section.heading(), section.level(), section.ordinal()))
                .toList();

        final SpecVersionDocument version = new SpecVersionDocument(
                selected.ordinal(),
                at(selected.createdAt()),
                selected.ordinal() == current.ordinal(),
                selected.content(),
                outline);
        version.setAuthor(selected.author());
        version.setCommitSha(selected.commitSha());

        final List<SpecVersionSummary> versionList = new ArrayList<>(all.size());
        for (final SpecVersion each : all) {
            final SpecVersionSummary summary =
                    new SpecVersionSummary(each.ordinal(), at(each.createdAt()), each.ordinal() == current.ordinal());
            summary.setAuthor(each.author());
            summary.setCommitSha(each.commitSha());
            versionList.add(summary);
        }

        final SpecDetail detail = new SpecDetail(
                document.id(),
                document.title(),
                document.path(),
                document.project(),
                document.repositoryFullName(),
                SpecStatus.fromValue(document.status().name()),
                new ArrayList<>(document.tags()),
                at(document.createdAt()),
                at(document.updatedAt()),
                version,
                versionList);
        detail.setDomain(document.domain());
        detail.setOwningTeam(document.owningTeam());
        detail.setOwner(document.owner());

        return detail;
    }

    private static OffsetDateTime at(final Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
