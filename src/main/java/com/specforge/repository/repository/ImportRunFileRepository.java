package com.specforge.repository.repository;

import com.specforge.repository.entity.ImportRunFileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunFileRepository extends JpaRepository<ImportRunFileEntity, UUID> {

    List<ImportRunFileEntity> findByRunIdOrderByPathAsc(UUID runId);
}
