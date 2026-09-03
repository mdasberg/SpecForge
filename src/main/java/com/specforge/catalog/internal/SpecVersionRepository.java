package com.specforge.catalog.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpecVersionRepository extends JpaRepository<SpecVersion, UUID> {

    /** The current version, which is the only one an import compares against. */
    Optional<SpecVersion> findFirstByDocumentIdOrderByOrdinalDesc(UUID documentId);

    List<SpecVersion> findByDocumentIdOrderByOrdinalAsc(UUID documentId);
}
