package com.specforge.repository;

import java.util.UUID;

/** Enough of a project to configure something per project and name it while doing so. */
public record ProjectRef(UUID projectId, String name) {}
