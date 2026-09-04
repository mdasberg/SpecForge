package com.specforge.catalog.service;

import com.specforge.platform.api.dto.SpecStatus;
import java.util.List;

/**
 * The filters a list request carried, normalised so the query builder never has to ask whether a
 * parameter was absent or merely empty. Every one of them is optional and they apply together as a
 * conjunction; a filter that accepts several values matches any of them.
 */
record Filters(
        List<String> statuses,
        List<String> owners,
        List<String> teams,
        List<String> domains,
        List<String> tags,
        String q) {

    static Filters of(final List<SpecStatus> statuses, final List<String> owners, final List<String> teams,
            final List<String> domains, final List<String> tags, final String q) {
        return new Filters(
                statuses == null ? List.of() : statuses.stream().map(SpecStatus::getValue).toList(),
                nullToEmpty(owners),
                nullToEmpty(teams),
                nullToEmpty(domains),
                nullToEmpty(tags),
                q == null || q.isBlank() ? null : q.strip());
    }

    boolean searching() {
        return q != null;
    }

    private static List<String> nullToEmpty(final List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> !value.isBlank()).toList();
    }
}
