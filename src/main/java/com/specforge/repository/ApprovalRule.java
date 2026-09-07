package com.specforge.repository;

import java.util.Set;

/**
 * A project's approval rule, as captured in the connect wizard's third step or edited afterwards.
 * This module stores it verbatim and never interprets it — the approval capability evaluates it.
 */
public record ApprovalRule(int minApprovals, Set<String> requiredRoles) {}
