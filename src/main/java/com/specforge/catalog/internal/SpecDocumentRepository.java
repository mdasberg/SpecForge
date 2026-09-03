package com.specforge.catalog.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpecDocumentRepository extends JpaRepository<SpecDocument, UUID> {

    Optional<SpecDocument> findByConnectionIdAndPath(UUID connectionId, String path);

    List<SpecDocument> findByConnectionId(UUID connectionId);
}
