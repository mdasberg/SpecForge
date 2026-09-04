package com.specforge.catalog.repository;

import com.specforge.catalog.entity.SpecSearchRow;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecSearchRepository extends JpaRepository<SpecSearchRow, UUID> {

    /** The index holds only the current version, so importing a new one clears the old rows first. */
    void deleteByDocumentId(UUID documentId);
}
