package com.specforge.repository.event;

import java.util.List;
import java.util.UUID;

/**
 * An import run has been recorded and is waiting to run. Published rather than called for the same
 * reason as {@link ScanRequested}: the runner loads the row on another thread once the transaction
 * that wrote it has committed.
 *
 * @param onlyPaths the paths to import, or null for everything the connection's glob matches
 */
public record ImportRequested(UUID runId, List<String> onlyPaths) {}
