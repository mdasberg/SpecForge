package com.specforge.repository.internal;

/** How far a repository scan has got. */
enum ScanStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED
}
