package com.specforge.repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The project a connection belongs to, for a capability whose configuration is per project rather
 * than per repository — automated checks, above all: which checks run and which of them block is a
 * team's decision, and a team's specifications can arrive through more than one connection.
 */
public interface Projects {

    Optional<ProjectRef> forConnection(UUID connectionId);
}
