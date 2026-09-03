package com.specforge.platform.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Turns raw query parameters into {@link ListParameters} in one place, because a sort or filter
 * property name reaches a query and every capability module would otherwise have to remember to
 * validate it itself.
 */
@Component
public class ListParametersParser {

    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;

    private static final String SORT = "sort";
    private static final String LIMIT = "limit";
    private static final String CURSOR = "cursor";
    private static final Pattern PROPERTY = Pattern.compile("[a-zA-Z][a-zA-Z0-9_.]*");

    public ListParameters parse(final Map<String, String> queryParameters) {
        return new ListParameters(
                parseSort(queryParameters.get(SORT)),
                parseFilters(queryParameters),
                parseLimit(queryParameters.get(LIMIT)),
                blankToNull(queryParameters.get(CURSOR)));
    }

    private static List<SortOrder> parseSort(final String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        final List<SortOrder> sort = new ArrayList<>();
        for (final String field : raw.split(",")) {
            final boolean descending = field.startsWith("-");
            final String property = descending ? field.substring(1) : field;
            sort.add(new SortOrder(validProperty(property), descending ? SortOrder.Direction.DESC : SortOrder.Direction.ASC));
        }
        return sort;
    }

    private static String validProperty(final String property) {
        if (!PROPERTY.matcher(property).matches()) {
            throw new IllegalArgumentException("Illegal property name: '" + property + "'");
        }
        return property;
    }

    private static int parseLimit(final String raw) {
        if (raw == null) {
            return DEFAULT_LIMIT;
        }
        int limit;
        try {
            limit = Integer.parseInt(raw);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Illegal limit: '" + raw + "'", e);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Illegal limit: '" + raw + "'");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static Map<String, String> parseFilters(final Map<String, String> queryParameters) {
        final Map<String, String> filters = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : queryParameters.entrySet()) {
            final String name = entry.getKey();
            if (SORT.equals(name) || LIMIT.equals(name) || CURSOR.equals(name)) {
                continue;
            }
            final String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                // A filter name reaches a query exactly like a sort property does, so it is held to
                // the same rule rather than trusted because it arrived under a different key.
                filters.put(validProperty(name), value);
            }
        }
        return filters;
    }

    private static String blankToNull(final String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }
}
