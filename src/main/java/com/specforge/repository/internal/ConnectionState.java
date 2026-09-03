package com.specforge.repository.internal;

/** Whether a connection still synchronises. A degraded connection keeps everything it imported readable. */
enum ConnectionState {
    ACTIVE,
    DEGRADED
}
