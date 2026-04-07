package sdk.humus.core;

import dev.humus.discovery.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.*;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sdk.humus.plugins.discovery.DiscoveryPlugin;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
public class HumusDriverErrorTest {

    @Container
    @Deprecated
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("humus_db")
            .withUsername("user")
            .withPassword("pass");

    private static Server grpcServer;
    private static final int GRPC_PORT = 9092;
    private static boolean shouldFailDiscovery = false;

    @BeforeAll
    static void setup() throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        grpcServer = ServerBuilder.forPort(GRPC_PORT)
                .addService(new DatabaseDiscoveryServiceGrpc.DatabaseDiscoveryServiceImplBase() {
                    @Override
                    public void getDatabaseInstance(DiscoveryRequest req, StreamObserver<DiscoveryResponse> obs) {
                        if (shouldFailDiscovery) {
                            obs.onError(Status.UNAVAILABLE
                                    .withDescription("Service Down")
                                    .asRuntimeException());
                            return;
                        }
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
        grpcServer.shutdownNow();
    }

    @BeforeEach
    void reset() {
        shouldFailDiscovery = false;
        DiscoveryPlugin.invalidateCache("error-cluster");
        DiscoveryPlugin.invalidateCache("wrong-db-cluster");
        DiscoveryPlugin.invalidateCache("humus_db");
    }

    @Test
    @DisplayName("Must throw SQLException when discovery service is unavailable")
    void shouldThrowExceptionWhenDiscoveryFails() {
        shouldFailDiscovery = true;
        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/error-cluster";

        SQLException ex = assertThrows(SQLException.class, () -> DriverManager.getConnection(url, "user", "pass"));
        assertTrue(ex.getMessage().contains("Discovery service unavailable"));
    }

    @Test
    @DisplayName("Must throw SQLException when database is down")
    void shouldThrowExceptionWhenDatabaseIsDown() {
        shouldFailDiscovery = true;
        String wrongDbCLuster = "/wrong-db-cluster";
        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + wrongDbCLuster;

        assertThrows(SQLException.class, () -> DriverManager.getConnection(url, "user", "pass"));
    }

    @Test
    @DisplayName("Should throw SQLException on SQL errors via StatementWrapper")
    void shouldForwardSqlExceptionsFromStatement() throws SQLException {
        shouldFailDiscovery = false;
        DiscoveryPlugin.invalidateCache("humus_db");

        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/humus_db";

        try (Connection conn = DriverManager.getConnection(url, "user", "pass");
                Statement stmt = conn.createStatement()) {

            SQLException ex =
                    assertThrows(SQLException.class, () -> stmt.executeQuery("SELECT * FROM non_existent_table"));

            assertFalse(
                    ex.getMessage().contains("Discovery service unavailable"),
                    "Should be SQL error, not Discovery error");
        }
    }

    @Test
    @DisplayName("Should return wrapped connection to enable plugins")
    void shouldReturnWrappedConnection() throws SQLException {

        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/humus_db";

        try (Connection conn = DriverManager.getConnection(url, "user", "pass")) {
            assertInstanceOf(
                    ConnectionWrapper.class,
                    conn,
                    "Connection must be an instance of ConnectionWrapper to support plugins!");

            try (Statement stmt = conn.createStatement()) {
                assertInstanceOf(StatementWrapper.class, stmt, "Statement must be wrapped to intercept SQL queries!");
            }
        }
    }
}
