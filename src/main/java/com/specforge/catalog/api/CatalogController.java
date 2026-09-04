package com.specforge.catalog.api;

import com.specforge.catalog.service.CatalogService;
import com.specforge.platform.api.dto.ProjectList;
import com.specforge.platform.api.dto.SpecDetail;
import com.specforge.platform.api.dto.SpecGrouping;
import com.specforge.platform.api.dto.SpecList;
import com.specforge.platform.api.dto.SpecStatus;
import com.specforge.platform.api.generated.CatalogApi;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * The catalog capability's HTTP surface. It implements the interface generated from
 * {@code specforge-api.yaml}, so a change to the contract that this does not follow fails to
 * compile rather than reaching a client.
 *
 * <p>There is no mapping here on purpose: the services speak the contract's own types, and this
 * class only routes.
 */
@RequiredArgsConstructor
@RestController
class CatalogController implements CatalogApi {

    private final CatalogService catalog;

    @Override
    public SpecList listSpecifications(
            final SpecGrouping groupBy,
            final List<SpecStatus> status,
            final List<String> owner,
            final List<String> team,
            final List<String> domain,
            final List<String> tag,
            final String q,
            final Integer limit,
            final String cursor) {
        return catalog.list(groupBy, status, owner, team, domain, tag, q, limit, cursor);
    }

    @Override
    public SpecDetail getSpecification(final UUID specId, final Integer version) {
        return catalog.get(specId, version);
    }

    @Override
    public ProjectList listProjects() {
        return catalog.projects();
    }
}
