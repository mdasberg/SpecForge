package com.specforge.discussion.repository;

import com.specforge.discussion.entity.ThreadEntity;
import com.specforge.review.AnchorState;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThreadRepository extends JpaRepository<ThreadEntity, UUID> {

    List<ThreadEntity> findByReviewIdOrderByResolvedAscCreatedAtDesc(UUID reviewId);

    /** Every thread still worth re-checking on a head advance; an orphaned one never un-orphans. */
    List<ThreadEntity> findByReviewIdAndAnchorStateNot(UUID reviewId, AnchorState anchorState);
}
