package com.specforge.repository;

import java.util.Optional;
import java.util.UUID;

/** The approval rule behind a connected repository, for a capability that has to evaluate it. */
public interface ApprovalRules {

    Optional<ApprovalRule> forConnection(UUID connectionId);
}
