package com.specforge.repository.service;

import java.util.List;

/**
 * The project configuration captured in the wizard's third step. The approval rule is stored here
 * and never interpreted: evaluating it belongs to the approval capability.
 */
public record ProjectCommand(
        String name,
        String team,
        List<String> domains,
        String tracker,
        String trackerProjectKey,
        int minApprovals,
        List<String> requiredRoles) {}
