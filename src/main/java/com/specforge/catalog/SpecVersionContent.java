package com.specforge.catalog;

import java.time.Instant;
import java.util.UUID;

/** One imported version, for a module that has to read a body rather than render one. */
public record SpecVersionContent(
        UUID documentId,
        int ordinal,
        String content,
        String contentSha,
        String commitSha,
        String author,
        Instant createdAt,
        boolean current) {}
