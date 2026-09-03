package com.specforge.catalog.repository;

import com.specforge.catalog.entity.SpecSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecSectionRepository extends JpaRepository<SpecSection, UUID> {

    List<SpecSection> findByVersionIdOrderByOrdinalAsc(UUID versionId);
}
