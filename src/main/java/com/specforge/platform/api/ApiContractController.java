package com.specforge.platform.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the API contract itself — the same document the server interfaces were generated from,
 * bundled to JSON at build time. It is deliberately not rebuilt from the running controllers: a
 * description derived from the implementation can drift from the contract consumers were given,
 * which is the whole failure this approach exists to prevent.
 */
@RestController
class ApiContractController {

    private static final ClassPathResource CONTRACT = new ClassPathResource("openapi/openapi.json");

    @GetMapping(value = "/api/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    String contract() throws IOException {
        return CONTRACT.getContentAsString(StandardCharsets.UTF_8);
    }
}
