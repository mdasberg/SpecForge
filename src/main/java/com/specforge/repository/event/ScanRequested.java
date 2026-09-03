package com.specforge.repository.event;

import java.util.UUID;

/**
 * A scan row has been written and is waiting to run. It is an event rather than a direct call so
 * the run starts after the transaction commits: the runner reads the row from another thread, and
 * a row that is still uncommitted simply is not there.
 */
public record ScanRequested(UUID scanId) {}
