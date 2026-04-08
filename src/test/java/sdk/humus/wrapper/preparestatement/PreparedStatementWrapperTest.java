package sdk.humus.wrapper.preparestatement;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sdk.humus.plugins.executiontime.ExecutionTimePlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class PreparedStatementWrapperTest {
    private ListAppender<ILoggingEvent> listAppender;

    @Container
    @Deprecated
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("prepared")
            .withUsername("user")
            .withPassword("pass");

    @BeforeEach
    void init() {
        Logger logger = (Logger) LoggerFactory.getLogger(ExecutionTimePlugin.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @Test
    @DisplayName("Test for PreparedStatementWrapper")
    void testPreparedStatementFlow() throws Exception {
        String url = "jdbc:humus:bublik://ignored-host:0/prepared";
        Properties props = new Properties();
        props.setProperty("user", postgres.getUsername());
        props.setProperty("password", postgres.getPassword());
        props.setProperty("slow-query-threshold-ms", "100");

        System.setProperty("test.postgres.host", postgres.getHost());
        System.setProperty("test.postgres.port", String.valueOf(postgres.getMappedPort(5432)));

        try (Connection conn = DriverManager.getConnection(url, props)) {

            String selectSql = "SELECT ? as val, pg_sleep(0.1)";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSql)) {
                pstmt.setInt(1, 42);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next());
                    assertEquals(42, rs.getInt("val"));
                }
            }
            assertLogContains("executeQuery", selectSql);
            listAppender.list.clear();

            conn.createStatement().execute("CREATE TABLE test_ps (id INT)");
            String insertSql = "INSERT INTO test_ps(id) VALUES ((SELECT ? FROM (SELECT pg_sleep(0.1)) a ))";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, 100);
                int rows = pstmt.executeUpdate();
                assertEquals(1, rows);
            }
            assertLogContains("executeUpdate", insertSql);
            listAppender.list.clear();

            String complexSql = "SELECT count(*), pg_sleep(0.1) FROM test_ps WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(complexSql)) {
                pstmt.setInt(1, 100);
                boolean hasResultSet = pstmt.execute();
                assertTrue(hasResultSet);
                try (ResultSet rs = pstmt.getResultSet()) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
            assertLogContains("execute", complexSql);
        }
    }

    private void assertLogContains(String actionDescription, String sqlSnippet) {
        String expectedPrefix = "SLOW QUERY DETECTED";

        boolean found = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(expectedPrefix)
                        && event.getFormattedMessage().contains(sqlSnippet));

        assertTrue(found, "The log doesn't contain the expected message for " + actionDescription + " with SQL snippet: " + actionDescription + ". Expected SQL: " + sqlSnippet);
    }
}
