package com.specforge.repository.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpecChangeProposalRepository extends JpaRepository<SpecChangeProposalEntity, UUID> {

    Optional<SpecChangeProposalEntity> findByConnectionIdAndPullRequestNumber(UUID connectionId, int pullRequestNumber);
}
