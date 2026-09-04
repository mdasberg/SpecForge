package com.specforge.platform.api;

/**
 * The pagination cursor every collection endpoint uses: the offset the next page starts at, as a
 * string, so the shape stays opaque to a client and can become something else without breaking one.
 *
 * <p>It is checked rather than trusted. A cursor arrives in a URL, and a URL is edited.
 */
public final class Cursors {

    private Cursors() {}

    public static int offsetOf(final String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            final int offset = Integer.parseInt(cursor.strip());
            if (offset < 0) {
                throw new NumberFormatException(cursor);
            }
            return offset;
        } catch (final NumberFormatException e) {
            throw Problems.badRequest("'%s' is not a cursor from a previous response.".formatted(cursor));
        }
    }

    /** The cursor for the page after this one, or null when this was the last page. */
    public static String next(final int offset, final int pageSize, final boolean hasMore) {
        return hasMore ? String.valueOf(offset + pageSize) : null;
    }
}
