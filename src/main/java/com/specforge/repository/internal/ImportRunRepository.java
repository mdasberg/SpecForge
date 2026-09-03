package com.specforge.repository.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ImportRunRepository extends JpaRepository<ImportRunEntity, UUID> {

    List<ImportRunEntity> findByConnectionIdOrderByStartedAtDesc(UUID connectionId);

    /** Scoped by connection so a run id from one connection cannot be read through another. */
    Optional<ImportRunEntity> findByIdAndConnectionId(UUID id, UUID connectionId);
}
