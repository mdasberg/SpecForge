package com.specforge;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.docs.Documenter;

/**
 * Generates the module graph into {@code build/spring-modulith-docs}. Run through the
 * {@code moduleDocs} Gradle task so the diagram is always derived from the code.
 */
class ModuleDocumentationTests {

    @Test
    void writesModuleDocumentation() {
        new Documenter(ModularityTests.MODULES)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeModuleCanvases();
    }
}
