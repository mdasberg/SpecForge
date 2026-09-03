package com.specforge.repository.internal;

/** When a connection re-imports. ON_PULL_REQUEST is the default because a pull request touching a spec is the moment a team wants review. */
enum SyncPolicy {
    ON_PULL_REQUEST,
    ON_PUSH,
    MANUAL
}
