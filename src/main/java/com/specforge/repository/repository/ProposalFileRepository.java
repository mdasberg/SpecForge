package com.specforge.repository.repository;

import com.specforge.repository.entity.ProposalFileEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProposalFileRepository extends JpaRepository<ProposalFileEntity, UUID> {

    List<ProposalFileEntity> findByProposalId(UUID proposalId);

    void deleteByProposalId(UUID proposalId);
}
