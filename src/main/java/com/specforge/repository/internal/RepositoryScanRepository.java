package com.specforge.repository.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RepositoryScanRepository extends JpaRepository<RepositoryScanEntity, UUID> {}
