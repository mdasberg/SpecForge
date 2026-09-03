package com.specforge.catalog.repository;

import com.specforge.catalog.entity.SpecDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SpecDocumentRepository extends JpaRepository<SpecDocument, UUID> {

    Optional<SpecDocument> findByConnectionIdAndPath(UUID connectionId, String path);

    List<SpecDocument> findByConnectionId(UUID connectionId);
}
