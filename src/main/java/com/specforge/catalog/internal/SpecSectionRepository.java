package com.specforge.catalog.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpecSectionRepository extends JpaRepository<SpecSection, UUID> {

    List<SpecSection> findByVersionIdOrderByOrdinalAsc(UUID versionId);
}
