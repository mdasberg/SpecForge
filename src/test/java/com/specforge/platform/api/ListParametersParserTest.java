package com.specforge.platform.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListParametersParserTest {

    private final ListParametersParser parser = new ListParametersParser();

    @Test
    void defaultsLimitWhenAbsent() {
        ListParameters parsed = parser.parse(Map.of());

        assertThat(parsed.limit()).isEqualTo(ListParametersParser.DEFAULT_LIMIT);
    }

    @Test
    void clampsLimitAboveMaximum() {
        ListParameters parsed = parser.parse(Map.of("limit", "9000"));

        assertThat(parsed.limit()).isEqualTo(ListParametersParser.MAX_LIMIT);
    }

    @Test
    void rejectsNonNumericLimit() {
        assertThatThrownBy(() -> parser.parse(Map.of("limit", "abc")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsZeroLimit() {
        assertThatThrownBy(() -> parser.parse(Map.of("limit", "0")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parsesAscendingAndDescendingSortWithPrefix() {
        ListParameters parsed = parser.parse(Map.of("sort", "-updatedAt,title"));

        assertThat(parsed.sort()).containsExactly(
                new SortOrder("updatedAt", SortOrder.Direction.DESC),
                new SortOrder("title", SortOrder.Direction.ASC));
    }

    @Test
    void rejectsIllegalPropertyName() {
        assertThatThrownBy(() -> parser.parse(Map.of("sort", "1bad")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1bad");
    }

    @Test
    void collectsFiltersExcludingReservedNames() {
        ListParameters parsed = parser.parse(Map.of(
                "sort", "title",
                "limit", "10",
                "cursor", "abc",
                "status", "approved",
                "author", "jane"));

        assertThat(parsed.filters()).isEqualTo(Map.of("status", "approved", "author", "jane"));
    }

    @Test
    void ignoresBlankFilterValues() {
        ListParameters parsed = parser.parse(Map.of("status", "   "));

        assertThat(parsed.filters()).isEmpty();
    }

    @Test
    void passesCursorThroughAndBlankBecomesNull() {
        assertThat(parser.parse(Map.of("cursor", "opaque-token")).cursor()).isEqualTo("opaque-token");
        assertThat(parser.parse(Map.of("cursor", "  ")).cursor()).isNull();
        assertThat(parser.parse(Map.of()).cursor()).isNull();
    }

    @Test
    void rejectsAnIllegalFilterName() {
        assertThatThrownBy(() -> parser.parse(Map.of("status;drop", "APPROVED")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status;drop");
    }
}
