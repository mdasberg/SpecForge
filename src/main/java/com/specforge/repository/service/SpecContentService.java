package com.specforge.repository.service;

import com.specforge.catalog.SpecCatalog;
import com.specforge.catalog.SpecLocation;
import com.specforge.platform.api.Problems;
import com.specforge.repository.entity.RepositoryConnectionEntity;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The read-only guarantee, made concrete. Editing a specification through the API is refused with
 * a conflict that names the repository and path the content actually lives at, so the author is
 * pointed at git rather than left wondering why the save button did nothing.
 *
 * <p>It lives in this module because only the repository connection knows the repository name
 * behind a specification.
 */
@RequiredArgsConstructor
@Service
@Transactional
public class SpecContentService {

    private final SpecCatalog catalog;
    private final RepositoryConnectionRepository connections;

    @Transactional(readOnly = true)
    public void refuseEdit(final UUID specId) {
        final SpecLocation location = catalog
                .locate(specId)
                .orElseThrow(() -> Problems.notFound("No specification %s.".formatted(specId)));
        final String repository = connections
                .findById(location.connectionId())
                .map(RepositoryConnectionEntity::repositoryFullName)
                .orElse("the connected repository");
        throw Problems.conflict(
                "This specification is a read-only mirror of %s in %s. Change the file in the repository; "
                        .formatted(location.path(), repository)
                        + "SpecForge never writes specification content back to git.");
    }
}
