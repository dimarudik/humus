package sdk.humus.spi.pluginorder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DirtiesContext
public class HumusOrderTest {
    @Container
    @Deprecated
    public PostgreSQLContainer<?> postgres_order =
            new PostgreSQLContainer<>("postgres").withUsername("user").withPassword("pass");

    @BeforeEach
    void clean() {
        OrderTracker.CALL_ORDER.clear();
    }

    @Test
    @DisplayName("Should execute plugins in correct order")
    void shouldExecutePluginsInCorrectOrder() throws Exception {
        System.setProperty("test.postgres.host", postgres_order.getHost());
        System.setProperty("test.postgres.port", String.valueOf(postgres_order.getMappedPort(5432)));
        System.setProperty("test.postgres.database", postgres_order.getDatabaseName());

        String url = "jdbc:humus:grpc://localhost:9091/" + postgres_order.getDatabaseName() + "?test=order-test";

        Properties props = new Properties();
        props.setProperty("user", "user");
        props.setProperty("password", "pass");

        try (Connection conn = DriverManager.getConnection(url, props);
                Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
        }

        assertTrue(OrderTracker.CALL_ORDER.size() >= 2, "Should have at least 2 plugins executed");

        int highIndex = OrderTracker.CALL_ORDER.indexOf("HighPriority");
        int lowIndex = OrderTracker.CALL_ORDER.indexOf("LowPriority");

        assertTrue(
                highIndex < lowIndex,
                "HighPriority (order 50) should be executed before LowPriority (order 900)" + ", but got: "
                        + OrderTracker.CALL_ORDER);

        System.out.println("Plugin execution order: " + OrderTracker.CALL_ORDER);
    }
}
