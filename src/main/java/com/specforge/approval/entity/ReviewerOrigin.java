package com.specforge.approval.entity;

/**
 * Where a required-reviewer slot came from. A {@code RULE} slot is created and removed by the
 * reconciler as the project's rule changes; an administrator's own {@link #MANUAL} addition is
 * never touched by that reconcile, only by the administrator who added it.
 */
public enum ReviewerOrigin {
    RULE,
    MANUAL
}
