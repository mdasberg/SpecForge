package com.specforge.catalog;

import java.util.Optional;
import java.util.UUID;

/**
 * What the rest of SpecForge may do to a specification document. Importing is the only way content
 * enters the catalogue — there is deliberately no operation that writes a body, because the
 * connected repository is the source of truth and SpecForge only mirrors it.
 */
public interface SpecCatalog {

    /**
     * Mirrors one file's current content. Creates the document on first sight, and a new version
     * only when the content hash differs from the current one, so re-importing an unchanged file
     * from a later commit is a no-op.
     */
    ImportResult importVersion(SpecImport specImport);

    /** Where a document came from, for anyone that has to name the source instead of writing to it. */
    Optional<SpecLocation> locate(UUID documentId);

    /** The document already mirroring a path, if that path has ever been imported. */
    Optional<UUID> find(UUID connectionId, String path);

    /**
     * Moves a document into review because a change was proposed for it. The transition is
     * refused if the document's current status does not allow it.
     */
    void proposeChange(UUID documentId);
}
