package com.specforge.catalog;

import java.util.Set;
import java.util.UUID;

/**
 * One file's state as an import found it. The title is not here: it is derived from the content's
 * first heading, so it cannot disagree with the document it describes.
 *
 * @param author the last commit author for this path, which is also taken as the document's owner
 */
public record SpecImport(
        UUID connectionId,
        String path,
        String project,
        String domain,
        String owningTeam,
        String author,
        Set<String> tags,
        String content,
        String commitSha) {}
