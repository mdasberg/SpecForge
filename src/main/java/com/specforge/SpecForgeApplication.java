package com.specforge;

import org.springframework.boot.SpringApplication;
import org.springframework.modulith.Modulith;

/**
 * SpecForge is a single deployable Spring Boot application whose capability modules are Spring
 * Modulith modules. {@code platform} is declared shared because every capability depends on the
 * API conventions, identity and error rendering it owns; declaring it here keeps the other nine
 * modules from each having to list it as an allowed dependency.
 */
@Modulith(systemName = "SpecForge", sharedModules = "platform")
public class SpecForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpecForgeApplication.class, args);
    }
}
