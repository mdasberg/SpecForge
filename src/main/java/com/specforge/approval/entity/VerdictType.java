package com.specforge.approval.entity;

/** What a cast verdict says. Persisted as its own name, deliberately not the contract's enum. */
public enum VerdictType {
    APPROVE,
    REQUEST_CHANGES,
    COMMENT
}
