package com.specforge;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * The module boundary is a test, not a review convention: a reference from one capability module
 * into another's internals fails the build here rather than being spotted by a human.
 */
class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(SpecForgeApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        MODULES.verify();
    }
}
