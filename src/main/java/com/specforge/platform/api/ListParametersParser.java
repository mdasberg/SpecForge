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

    public ListParameters parse(Map<String, String> queryParameters) {
        return new ListParameters(
                parseSort(queryParameters.get(SORT)),
                parseFilters(queryParameters),
                parseLimit(queryParameters.get(LIMIT)),
                blankToNull(queryParameters.get(CURSOR)));
    }

    private static List<SortOrder> parseSort(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<SortOrder> sort = new ArrayList<>();
        for (String field : raw.split(",")) {
            boolean descending = field.startsWith("-");
            String property = descending ? field.substring(1) : field;
            sort.add(new SortOrder(validProperty(property), descending ? SortOrder.Direction.DESC : SortOrder.Direction.ASC));
        }
        return sort;
    }

    private static String validProperty(String property) {
        if (!PROPERTY.matcher(property).matches()) {
            throw new IllegalArgumentException("Illegal property name: '" + property + "'");
        }
        return property;
    }

    private static int parseLimit(String raw) {
        if (raw == null) {
            return DEFAULT_LIMIT;
        }
        int limit;
        try {
            limit = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal limit: '" + raw + "'", e);
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("Illegal limit: '" + raw + "'");
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static Map<String, String> parseFilters(Map<String, String> queryParameters) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
            String name = entry.getKey();
            if (SORT.equals(name) || LIMIT.equals(name) || CURSOR.equals(name)) {
                continue;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                // A filter name reaches a query exactly like a sort property does, so it is held to
                // the same rule rather than trusted because it arrived under a different key.
                filters.put(validProperty(name), value);
            }
        }
        return filters;
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw;
    }
}
