package com.specforge.repository.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScanFileRepository extends JpaRepository<ScanFileEntity, UUID> {

    List<ScanFileEntity> findByScanIdOrderByPathAsc(UUID scanId);
}
