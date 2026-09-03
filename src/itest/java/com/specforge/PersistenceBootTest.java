package com.specforge;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Guards that the composed PostgreSQL is what the application actually boots against, so an
 * accidental in-memory fallback fails the build.
 */
class PersistenceBootTest extends BaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void bootsAgainstRealPostgres() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
        }
    }

    /**
     * Liquibase auto-configuration lives in its own module on Spring Boot 4, so leaving it off the
     * classpath makes every migration silently skip rather than fail. Only the changelog tables
     * prove it actually ran.
     */
    @Test
    void appliesLiquibaseMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData()
                        .getTables(null, null, "databasechangelog", null)) {
            assertThat(tables.next()).as("Liquibase changelog table").isTrue();
        }
    }
}
