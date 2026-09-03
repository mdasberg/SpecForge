package com.specforge.repository.entity;

/** Whether an installation can still be read through. A suspended or revoked installation degrades every connection made through it. */
public enum InstallationStatus {
    ACTIVE,
    SUSPENDED,
    REVOKED
}
