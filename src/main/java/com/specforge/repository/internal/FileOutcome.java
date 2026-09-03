package com.specforge.repository.internal;

/** What happened to one file during an import run. */
enum FileOutcome {
    IMPORTED,
    UNCHANGED,
    SKIPPED,
    FAILED
}
