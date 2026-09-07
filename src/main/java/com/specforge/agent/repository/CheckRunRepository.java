package com.specforge.agent.repository;

import com.specforge.agent.entity.CheckRunEntity;
import com.specforge.agent.entity.RunState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckRunRepository extends JpaRepository<CheckRunEntity, UUID> {

    /** The Checks tab: what is true of the head under review, oldest run first. */
    List<CheckRunEntity> findByReviewIdAndStaleFalseOrderByQueuedAtAsc(UUID reviewId);

    List<CheckRunEntity> findByReviewIdOrderByQueuedAtAsc(UUID reviewId);

    List<CheckRunEntity> findByReviewIdAndStaleFalseAndStateIn(UUID reviewId, List<RunState> states);
}
