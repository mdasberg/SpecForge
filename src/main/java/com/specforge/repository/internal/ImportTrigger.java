package com.specforge.repository.internal;

/** What caused an import run. */
enum ImportTrigger {
    INITIAL,
    PUSH,
    PULL_REQUEST,
    MANUAL
}
