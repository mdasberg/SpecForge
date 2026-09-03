package com.specforge.repository.forge;

/** A repository and the branch or commit to read it at. */
public record ForgeRef(String repositoryFullName, String ref) {}
