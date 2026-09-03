package com.specforge.repository.repository;

import com.specforge.repository.entity.RepositoryScanEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;


public interface RepositoryScanRepository extends JpaRepository<RepositoryScanEntity, UUID> {}
