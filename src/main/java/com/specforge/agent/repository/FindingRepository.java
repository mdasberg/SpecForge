package com.specforge.agent.repository;

import com.specforge.agent.entity.FindingEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<FindingEntity, UUID> {

    List<FindingEntity> findByCheckRunIdInOrderByCreatedAtAsc(Collection<UUID> checkRunIds);

    List<FindingEntity> findByReviewIdOrderByCreatedAtAsc(UUID reviewId);
}
