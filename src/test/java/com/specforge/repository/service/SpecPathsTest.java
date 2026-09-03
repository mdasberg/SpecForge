package com.specforge.repository.service;

import com.specforge.repository.service.SpecPaths;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



class SpecPathsTest {

    @Test
    void matchesTheDefaultGlobAtAnyDepth() {
        List<String> matched = SpecPaths.matching(
                List.of(
                        "openspec/specs/billing/spec.md",
                        "openspec/specs/care/claims/spec.md",
                        "openspec/specs/billing/design.md",
                        "README.md"),
                "openspec/specs/**/spec.md");

        assertThat(matched).containsExactly("openspec/specs/billing/spec.md", "openspec/specs/care/claims/spec.md");
    }

    @Test
    void readsTheDomainFromTheDirectoryHoldingTheFile() {
        assertThat(SpecPaths.domainOf("openspec/specs/billing/spec.md")).isEqualTo("billing");
        assertThat(SpecPaths.domainOf("spec.md")).isNull();
    }
}
