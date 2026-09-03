package com.specforge.repository.repository;

import com.specforge.repository.entity.SpecChangeProposalEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface SpecChangeProposalRepository extends JpaRepository<SpecChangeProposalEntity, UUID> {

    Optional<SpecChangeProposalEntity> findByConnectionIdAndPullRequestNumber(UUID connectionId, int pullRequestNumber);
}
