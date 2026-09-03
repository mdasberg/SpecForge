package com.specforge.catalog;

import java.util.UUID;

/**
 * What an import did to one document.
 *
 * @param versionCreated false when the content hash matched the current version, which is the
 *     signal an import run records as UNCHANGED rather than IMPORTED
 */
public record ImportResult(UUID documentId, int ordinal, boolean versionCreated) {}
