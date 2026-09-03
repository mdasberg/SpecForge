package com.specforge.repository.entity;

/** What caused an import run. */
public enum ImportTrigger {
    INITIAL,
    PUSH,
    PULL_REQUEST,
    MANUAL
}
