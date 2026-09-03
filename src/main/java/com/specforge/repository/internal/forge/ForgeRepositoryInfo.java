package com.specforge.repository.internal.forge;

/** A repository an installation grants access to. */
public record ForgeRepositoryInfo(String fullName, String defaultBranch, String externalId) {}
