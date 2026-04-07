package sdk.humus.spi.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sdk.humus.core.ConnectionWrapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DirtiesContext
public class HumusSpiIntegrationTest {
    @Container
    @Deprecated
    public PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("spi_db")
            .withUsername("user")
            .withPassword("pass");

    @BeforeAll
    static void setup() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
    }

    @Test
    @DisplayName("Should load custom plugin via SPI")
    void shouldLoadCustomPluginViaSpi() throws Exception {
        Class.forName("sdk.humus.HumusDriver");

        String customUrl = "jdbc:humus:custom-spi://ignored-host:0/spi_db";

        Properties props = new Properties();
        props.setProperty("user", "user");
        props.setProperty("password", "pass");
        props.setProperty("humus.slow-query-threshold-ms", "1000");

        System.setProperty("test.postgres.host", postgres.getHost());
        System.setProperty("test.postgres.port", String.valueOf(postgres.getMappedPort(5432)));

        try (Connection conn = DriverManager.getConnection(customUrl, props)) {
            Statement stmt = conn.createStatement();
            stmt.execute("select 1, pg_sleep(1)");
            stmt.close();
            assertNotNull(conn);
            assertTrue(conn instanceof ConnectionWrapper);

            String realUrl = conn.getMetaData().getURL();
            assertTrue(realUrl.contains(postgres.getHost()));

            System.out.println("SPI Custom Plugin successfully resolved URL to: " + realUrl);
        }
    }
}
