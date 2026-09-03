package com.specforge.catalog;

/**
 * The lifecycle of a specification document: Draft, In Review, Changes Requested, Approved,
 * Implemented. The legal transitions between these are enforced in one place inside this module,
 * not by whoever happens to be setting the field.
 */
public enum SpecStatus {
    DRAFT,
    IN_REVIEW,
    CHANGES_REQUESTED,
    APPROVED,
    IMPLEMENTED
}
