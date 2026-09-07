package com.specforge.approval.entity;

/** A required reviewer's state. Persisted as its own name, deliberately not the contract's enum. */
public enum ReviewerState {
    PENDING,
    APPROVED,
    CHANGES_REQUESTED
}
