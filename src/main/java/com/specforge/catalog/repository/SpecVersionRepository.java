package com.specforge.catalog.repository;

import com.specforge.catalog.entity.SpecVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecVersionRepository extends JpaRepository<SpecVersion, UUID> {

    /** The current version, which is the only one an import compares against. */
    Optional<SpecVersion> findFirstByDocumentIdOrderByOrdinalDesc(UUID documentId);

    List<SpecVersion> findByDocumentIdOrderByOrdinalAsc(UUID documentId);

    Optional<SpecVersion> findByDocumentIdAndOrdinal(UUID documentId, int ordinal);
}
