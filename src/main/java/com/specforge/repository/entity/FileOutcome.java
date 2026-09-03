package com.specforge.repository.entity;

/** What happened to one file during an import run. */
public enum FileOutcome {
    IMPORTED,
    UNCHANGED,
    SKIPPED,
    FAILED
}
