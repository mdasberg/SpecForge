package com.specforge.repository.internal;

/** Whether an installation can still be read through. A suspended or revoked installation degrades every connection made through it. */
enum InstallationStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED
}
