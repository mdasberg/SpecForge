package com.specforge.repository.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImportRunFileRepository extends JpaRepository<ImportRunFileEntity, UUID> {

    List<ImportRunFileEntity> findByRunIdOrderByPathAsc(UUID runId);
}
