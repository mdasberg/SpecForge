package com.specforge.approval.repository;

import com.specforge.approval.entity.VerdictEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerdictRepository extends JpaRepository<VerdictEntity, UUID> {

    /** The verdict log shown on the panel: only ever the current head's, not every head's history. */
    List<VerdictEntity> findByReviewIdAndHeadContentShaOrderByCreatedAtDesc(UUID reviewId, String headContentSha);

    /** Recasting against the same head updates this row rather than adding a second one. */
    Optional<VerdictEntity> findByReviewIdAndSubjectIdAndHeadContentSha(
            UUID reviewId, String subjectId, String headContentSha);
}
