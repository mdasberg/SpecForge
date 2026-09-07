package com.specforge.agent.entity;

/**
 * How a check reaches its verdict. Persisted here rather than taken from the contract so that
 * renaming the API value stays an API change instead of a data migration.
 */
public enum RunnerKind {
    DETERMINISTIC,
    MODEL
}
