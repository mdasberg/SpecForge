package com.specforge.agent.repository;

import com.specforge.agent.entity.FindingDispositionEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingDispositionRepository extends JpaRepository<FindingDispositionEntity, UUID> {

    /** Oldest first, so the last row for a finding is its current disposition. */
    List<FindingDispositionEntity> findByFindingIdInOrderByCreatedAtAsc(Collection<UUID> findingIds);
}
