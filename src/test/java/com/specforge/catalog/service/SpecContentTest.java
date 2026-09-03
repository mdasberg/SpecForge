package com.specforge.catalog.service;

import com.specforge.catalog.service.SpecContent;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



class SpecContentTest {

    @Test
    void hashesLineEndingAndWhitespaceVariantsIdentically() {
        String unix = "# Spec\n\nBody.\n";
        String windows = "# Spec\r\n\r\nBody.   \r\n\r\n\r\n";

        assertThat(SpecContent.sha256(SpecContent.normalise(windows)))
                .isEqualTo(SpecContent.sha256(SpecContent.normalise(unix)));
    }

    @Test
    void hashesChangedContentDifferently() {
        assertThat(SpecContent.sha256(SpecContent.normalise("# Spec\n\nOne.\n")))
                .isNotEqualTo(SpecContent.sha256(SpecContent.normalise("# Spec\n\nTwo.\n")));
    }

    @Test
    void endsNormalisedContentWithExactlyOneNewline() {
        assertThat(SpecContent.normalise("# Spec\n\n\n")).isEqualTo("# Spec\n");
        assertThat(SpecContent.normalise("")).isEmpty();
    }
}
