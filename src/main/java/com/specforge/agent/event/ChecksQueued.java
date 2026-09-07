package com.specforge.agent.event;

import java.util.List;
import java.util.UUID;

/**
 * Runs have been recorded as queued and are waiting to execute. Published rather than called for the
 * same reason the repository's import is: a runner on another thread cannot see rows the writing
 * transaction has not committed, so it is told once the commit has happened.
 */
public record ChecksQueued(UUID reviewId, List<UUID> runIds) {}
