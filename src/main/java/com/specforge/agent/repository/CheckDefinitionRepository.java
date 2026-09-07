package com.specforge.agent.repository;

import com.specforge.agent.entity.CheckDefinitionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckDefinitionRepository extends JpaRepository<CheckDefinitionEntity, UUID> {

    List<CheckDefinitionEntity> findByProjectIdOrderByDisplayNameAsc(UUID projectId);

    /** Only the enabled ones are dispatched: a disabled check is not skipped work, it is no work. */
    List<CheckDefinitionEntity> findByProjectIdAndEnabledTrueOrderByDisplayNameAsc(UUID projectId);
}
