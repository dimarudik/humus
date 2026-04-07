package sdk.humus.executiontime.core;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.humus.discovery.DatabaseDiscoveryServiceGrpc;
import dev.humus.discovery.DiscoveryRequest;
import dev.humus.discovery.DiscoveryResponse;
import dev.humus.discovery.InstanceType;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sdk.humus.plugins.discovery.DiscoveryPlugin;
import sdk.humus.plugins.executiontime.ExecutionTimeConstants;
import sdk.humus.plugins.executiontime.ExecutionTimePlugin;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class CoreExecutionTimePluginTest {
    private ListAppender<ILoggingEvent> listAppender;

    @Container
    @Deprecated
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("humus_db")
            .withUsername("user")
            .withPassword("pass");

    private static Server grpcServer;
    private static final AtomicInteger discoveryCalls = new AtomicInteger(0);
    private static final int GRPC_PORT = 9091;

    @BeforeAll
    static void setup() throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);

        grpcServer = ServerBuilder.forPort(GRPC_PORT)
                .addService(new DatabaseDiscoveryServiceGrpc.DatabaseDiscoveryServiceImplBase() {
                    @Override
                    public void getDatabaseInstance(DiscoveryRequest req, StreamObserver<DiscoveryResponse> obs) {
                        discoveryCalls.incrementAndGet();
                        obs.onNext(DiscoveryResponse.newBuilder()
                                .setHost(postgres.getHost())
                                .setPort(postgres.getMappedPort(5432))
                                .setInstanceType(InstanceType.MASTER)
                                .build());
                        obs.onCompleted();
                    }
                })
                .build()
                .start();
    }

    @AfterAll
    static void tearDown() {
        if (grpcServer != null) grpcServer.shutdownNow();
    }

    @BeforeEach
    void init() {
        DiscoveryPlugin.clearCache();
        discoveryCalls.set(0);

        Logger logger = (Logger) LoggerFactory.getLogger(ExecutionTimePlugin.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Тестируем ExecutionTimePlugin с настройкой порога")
    void testExecutionTime() throws Exception {
        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/humus_db";

        Properties props = new Properties();
        props.setProperty("user", "user");
        props.setProperty("password", "pass");
        props.setProperty("slow-query-threshold-ms", "1000");

        try (Connection conn = DriverManager.getConnection(url, props)) {

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT /* Statement 1 */ 1, pg_sleep(0.5)");
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
            assertTrue(listAppender.list.isEmpty(), "Log should be empty");

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT /* Statement 2 */ 2, pg_sleep(1.5)");
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }

            String psSql = "SELECT /* PreparedStatement */ pg_sleep(?)";
            try (PreparedStatement pstmt = conn.prepareStatement(psSql)) {
                pstmt.setDouble(1, 1.3);
                pstmt.execute();
            }

            String csSql = "select /* CallableStatement */ pg_sleep(?)";
            try (CallableStatement cstmt = conn.prepareCall(csSql)) {
                cstmt.setDouble(1, 1.1);
                cstmt.execute();
            }
        }
        assertFalse(listAppender.list.isEmpty(), "Log should not be empty");
        assertTrue(
                listAppender
                        .list
                        .getFirst()
                        .getFormattedMessage()
                        .startsWith(ExecutionTimeConstants.LOG_TIME_TEMPLATE.split("\\{0}")[0]),
                "Log message should start with template:"
                        + ExecutionTimeConstants.LOG_TIME_TEMPLATE.split("\\{0}")[0]);
    }
}
