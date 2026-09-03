package com.specforge.repository.entity;

/** Whether a connection still synchronises. A degraded connection keeps everything it imported readable. */
public enum ConnectionState {
    ACTIVE,
    DEGRADED
}
