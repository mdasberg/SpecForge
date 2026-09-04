package com.specforge.review.entity;

/**
 * Persisted as its own name, and deliberately not the contract's enum: renaming a value in the API
 * document must stay an API change rather than turning into a data migration.
 */
public enum ReviewState {
    OPEN,
    CLOSED
}
