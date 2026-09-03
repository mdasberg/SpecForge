package com.specforge.repository.internal;

import java.util.List;
import java.util.UUID;

/** A request to connect a repository, as the module understands it. */
record ConnectionCommand(
        UUID installationId,
        String repositoryFullName,
        String branch,
        String pathGlob,
        SpecFileFormat specFormat,
        SyncPolicy syncMode,
        UUID scanId,
        ProjectCommand project) {}
