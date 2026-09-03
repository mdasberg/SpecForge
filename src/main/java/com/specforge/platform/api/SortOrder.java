package com.specforge.platform.api;

/**
 * One field in a list request's sort, kept as a small type of its own so query builders across
 * capability modules read the same "property plus direction" shape instead of a raw string.
 */
public record SortOrder(String property, Direction direction) {

    public enum Direction {
        ASC,
        DESC
    }
}
