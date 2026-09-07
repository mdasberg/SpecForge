package com.specforge.discussion.service;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MentionParserTest {

    @Test
    void findsOneHandle() {
        assertThat(MentionParser.handles("agree @amara.okoth?")).containsExactly("amara.okoth");
    }

    @Test
    void findsEveryDistinctHandle() {
        assertThat(MentionParser.handles("cc @architect and @admin-user, also @architect again"))
                .containsExactly("architect", "admin-user");
    }

    @Test
    void ignoresAnEmailAddressLocalPart() {
        assertThat(MentionParser.handles("contact reviewer@example.com about this")).isEmpty();
    }

    @Test
    void noHandlesIsAnEmptySet() {
        assertThat(MentionParser.handles("no mentions here")).isEqualTo(Set.of());
    }
}
