package com.specforge.repository.repository;

import com.specforge.repository.entity.RepositoryConnectionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RepositoryConnectionRepository extends JpaRepository<RepositoryConnectionEntity, UUID> {

    /** The uniqueness the wizard enforces: one connection per repository, branch and path. */
    Optional<RepositoryConnectionEntity> findByRepositoryFullNameAndBranchAndPathGlob(
            String repositoryFullName, String branch, String pathGlob);

    List<RepositoryConnectionEntity> findByRepositoryFullNameAndBranch(String repositoryFullName, String branch);

    List<RepositoryConnectionEntity> findByRepositoryFullName(String repositoryFullName);

    List<RepositoryConnectionEntity> findByInstallationId(UUID installationId);

    List<RepositoryConnectionEntity> findAllByOrderByCreatedAtAsc();
}
