package com.specforge.repository.forge;

/** A repository an installation grants access to. */
public record ForgeRepositoryInfo(String fullName, String defaultBranch, String externalId) {}
