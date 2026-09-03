package com.specforge.platform.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The parsed form of a list request, so a repository method takes one typed argument instead of
 * every capability module re-reading {@code sort}, filter and paging parameters off the request.
 */
public record ListParameters(List<SortOrder> sort, Map<String, String> filters, int limit, String cursor) {

    public ListParameters {
        sort = List.copyOf(sort);
        // Map.copyOf does not preserve encounter order; filters are surfaced in the order the
        // caller sent them, so the copy has to go through a LinkedHashMap instead.
        filters = Collections.unmodifiableMap(new LinkedHashMap<>(filters));
    }
}
