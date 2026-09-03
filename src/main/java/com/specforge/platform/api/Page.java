package com.specforge.platform.api;

import java.util.List;

/**
 * The envelope every list endpoint returns, so a client parses "items plus a total plus a next
 * cursor" once rather than once per capability module.
 */
public record Page<T>(List<T> items, long total, String cursor) {

    public Page {
        items = List.copyOf(items);
    }

    public static <T> Page<T> of(List<T> items, long total, String cursor) {
        return new Page<>(items, total, cursor);
    }

    /** A complete, un-paged result: the total is simply how many items there are. */
    public static <T> Page<T> of(List<T> items) {
        return new Page<>(items, items.size(), null);
    }
}
