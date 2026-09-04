package com.specforge.repository.service;

import com.specforge.repository.service.SpecPaths;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;



class SpecPathsTest {

    @Test
    void matchesADeeperLayoutWithItsOwnGlob() {
        // `openspecs`, not `openspec`: the default glob matches none of these, which is why a
        // connection to such a repository has to say so rather than silently importing nothing.
        assertThat(SpecPaths.matching(List.of("openspecs/specs/clm/claim/spec.md"), "openspec/specs/**/spec.md"))
                .isEmpty();
        assertThat(SpecPaths.matching(List.of("openspecs/specs/clm/claim/spec.md"), "openspecs/specs/**/spec.md"))
                .containsExactly("openspecs/specs/clm/claim/spec.md");
    }

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
    void readsTheDomainFromTheSegmentUnderSpecs() {
        assertThat(SpecPaths.domainOf("openspec/specs/billing/spec.md")).isEqualTo("billing");
        // A repository that groups its specifications one level deeper still reports the grouping
        // as the domain, not the capability — otherwise every specification is its own domain and
        // grouping by domain says nothing.
        assertThat(SpecPaths.domainOf("openspecs/specs/clm/claim/spec.md")).isEqualTo("clm");
        assertThat(SpecPaths.domainOf("openspecs/specs/fin/payment/spec.md")).isEqualTo("fin");
    }

    @Test
    void takesTheInnermostSpecsDirectory() {
        assertThat(SpecPaths.domainOf("docs/specs/archive/specs/mem/policy/spec.md")).isEqualTo("mem");
    }

    @Test
    void fallsBackToTheDirectoryHoldingTheFileWhenThereIsNoSpecsDirectory() {
        assertThat(SpecPaths.domainOf("documentation/billing/spec.md")).isEqualTo("billing");
    }

    @Test
    void hasNoDomainWhenThePathCarriesNothingToNameOneWith() {
        assertThat(SpecPaths.domainOf("spec.md")).isNull();
        assertThat(SpecPaths.domainOf("openspec/specs/spec.md")).isNull();
    }
}
