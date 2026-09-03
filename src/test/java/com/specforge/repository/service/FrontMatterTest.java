package com.specforge.repository.service;

import com.specforge.repository.service.FrontMatter;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



class FrontMatterTest {

    @Test
    void readsAnInlineTagList() {
        assertThat(FrontMatter.tags("---\ntags: [billing, \"claims\"]\n---\n# Spec\n"))
                .containsExactlyInAnyOrder("billing", "claims");
    }

    @Test
    void readsABlockTagList() {
        assertThat(FrontMatter.tags("---\ntitle: Spec\ntags:\n  - billing\n  - claims\n---\n# Spec\n"))
                .containsExactlyInAnyOrder("billing", "claims");
    }

    @Test
    void hasNoTagsWithoutFrontMatter() {
        assertThat(FrontMatter.tags("# Spec\n\ntags: not front matter\n")).isEmpty();
    }
}
