package com.specforge;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Shared base for the `itest` source set, which is the integration suite: application.yaml
 * already points at the docker-compose PostgreSQL, never H2, and every subclass shares one cached
 * Spring context.
 */
@SpringBootTest
public abstract class BaseIntegrationTest {
}
