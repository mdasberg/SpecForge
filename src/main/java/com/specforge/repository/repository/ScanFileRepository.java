package com.specforge.repository.repository;

import com.specforge.repository.entity.ScanFileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ScanFileRepository extends JpaRepository<ScanFileEntity, UUID> {

    List<ScanFileEntity> findByScanIdOrderByPathAsc(UUID scanId);
}
