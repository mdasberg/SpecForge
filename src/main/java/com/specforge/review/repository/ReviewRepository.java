package com.specforge.review.repository;

import com.specforge.review.entity.ReviewEntity;
import com.specforge.review.entity.ReviewState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    /**
     * The one review a specification may have open. The database enforces the "one" with a partial
     * unique index, so two concurrent webhook deliveries cannot both win.
     */
    Optional<ReviewEntity> findByDocumentIdAndState(UUID documentId, ReviewState state);

    List<ReviewEntity> findByProposalId(UUID proposalId);
}
