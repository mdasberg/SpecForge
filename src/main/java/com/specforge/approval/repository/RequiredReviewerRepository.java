package com.specforge.approval.repository;

import com.specforge.approval.entity.ReviewerOrigin;
import com.specforge.approval.entity.RequiredReviewerEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequiredReviewerRepository extends JpaRepository<RequiredReviewerEntity, UUID> {

    List<RequiredReviewerEntity> findByReviewIdAndRemovedAtIsNullOrderByAddedAtAsc(UUID reviewId);

    /** Every live seat, reset to pending on a head advance regardless of origin. */
    List<RequiredReviewerEntity> findByReviewIdAndRemovedAtIsNull(UUID reviewId);

    /** The rule-derived seats the reconciler is allowed to add to or remove from. */
    List<RequiredReviewerEntity> findByReviewIdAndOriginAndRemovedAtIsNull(UUID reviewId, ReviewerOrigin origin);
}
