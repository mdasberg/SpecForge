package com.specforge.repository.service;

import com.specforge.repository.entity.SpecFileFormat;
import com.specforge.repository.entity.SyncPolicy;
import java.util.List;
import java.util.UUID;


/** A request to connect a repository, as the module understands it. */
public record ConnectionCommand(
        UUID installationId,
        String repositoryFullName,
        String branch,
        String pathGlob,
        SpecFileFormat specFormat,
        SyncPolicy syncMode,
        UUID scanId,
        ProjectCommand project) {}
