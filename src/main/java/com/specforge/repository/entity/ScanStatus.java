package com.specforge.repository.entity;

/** How far a repository scan has got. */
public enum ScanStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
