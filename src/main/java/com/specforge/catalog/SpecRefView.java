package com.specforge.catalog;

import java.util.UUID;

/**
 * Enough of a specification to name it and link to it. Modules that list something belonging to a
 * specification — a review, later a ticket — need the title and path beside their own row, and this
 * is how they get it without either denormalising the catalogue into their tables or reaching into
 * them.
 */
public record SpecRefView(
        UUID id,
        String title,
        String path,
        String project,
        String repositoryFullName,
        SpecStatus status) {}
