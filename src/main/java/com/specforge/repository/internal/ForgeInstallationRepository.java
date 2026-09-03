package com.specforge.repository.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ForgeInstallationRepository extends JpaRepository<ForgeInstallationEntity, UUID> {

    /** Keyed by the forge's own id: it is what a webhook carries, and it survives a rename. */
    Optional<ForgeInstallationEntity> findByExternalId(String externalId);

    List<ForgeInstallationEntity> findAllByOrderByAccountLoginAsc();
}
