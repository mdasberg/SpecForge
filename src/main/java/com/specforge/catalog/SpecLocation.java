package com.specforge.catalog;

import java.util.UUID;

/** The connection and repository path a document mirrors. */
public record SpecLocation(UUID connectionId, String path) {}
