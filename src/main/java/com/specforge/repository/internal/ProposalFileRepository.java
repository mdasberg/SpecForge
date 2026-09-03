package com.specforge.repository.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProposalFileRepository extends JpaRepository<ProposalFileEntity, UUID> {

    List<ProposalFileEntity> findByProposalId(UUID proposalId);

    void deleteByProposalId(UUID proposalId);
}
