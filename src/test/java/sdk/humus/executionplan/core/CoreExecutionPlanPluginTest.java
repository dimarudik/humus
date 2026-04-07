package sdk.humus.executionplan.core;

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
import java.util.logging.Level;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import sdk.humus.plugins.discovery.DiscoveryPlugin;
import sdk.humus.plugins.executionplan.ExecutionPlanPlugin;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class CoreExecutionPlanPluginTest {
    private ListAppender<ILoggingEvent> listAppender;

    @Container
    @Deprecated
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("humus_db")
            .withUsername("user")
            .withPassword("pass");
    /*
                .withCommand("postgres",
                        "-c", "log_min_duration_statement=0",
                        "-c", "log_statement=all")
                .withLogConsumer(frame -> System.out.print(frame.getUtf8String()));
    */

    private static Server grpcServer;
    private static final AtomicInteger discoveryCalls = new AtomicInteger(0);
    private static final int GRPC_PORT = 9091;

    @BeforeAll
    static void setup() throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

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

        Logger logger = (Logger) LoggerFactory.getLogger(ExecutionPlanPlugin.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    @DisplayName("Тестируем ExecutionPlanPlugin с настройкой порога")
    void testExecutionTime() throws Exception {
        String url = "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/humus_db";

        Properties props = new Properties();
        props.setProperty("user", "user");
        props.setProperty("password", "pass");
        //        props.setProperty("slow-query-threshold-ms", "500");
        props.setProperty("slow-query-plan-threshold-ms", "1000");

        try (Connection conn = DriverManager.getConnection(url, props)) {

            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT /* Statement 1 */ 1, pg_sleep(0.8)");
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }

            // чтобы получить план в логе нужно закрыть ResultSet либо через close или через try-with-resources
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT /* Statement 2 */ 2, pg_sleep(1.2)");
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
                rs.close();
            }
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT /* Statement 3 */ 2, pg_sleep(1.2)")) {
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }

            // тут execute возвращает boolean, а не ResultSet, поэтому логирование происходит всегда
            String psSql = "SELECT /* PreparedStatement */ pg_sleep(?)";
            try (PreparedStatement pstmt = conn.prepareStatement(psSql)) {
                pstmt.setDouble(1, 1.3);
                pstmt.execute();
            }

            // тут execute возвращает boolean, а не ResultSet, поэтому логирование происходит всегда
            String csSql = "select /* CallableStatement */ pg_sleep(?)";
            try (CallableStatement cstmt = conn.prepareCall(csSql)) {
                cstmt.setDouble(1, 1.1);
                cstmt.execute();
            }

            // тут execute возвращает boolean для update, а не ResultSet, поэтому логирование происходит всегда
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("create table test (id int, v int)");
                stmt.execute("insert into test values (1, 1)");
                stmt.execute(
                        "update test set v = (CASE WHEN pg_sleep(1.3) IS NULL THEN v + 1 ELSE v + 1 END) where id = 1");
                ResultSet rs = stmt.executeQuery("SELECT v from test where id = 1");
                assertTrue(rs.next());
                assertEquals(2, rs.getInt(1));
            }

            String longReadSql =
                    "select a.oid, b.oid, c.oid from (select oid::int from pg_class limit ?) a, (select oid::int from pg_class limit ?) b, (select oid::int from pg_class limit ?) c";
            //            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(longReadSql)) {
                stmt.setFetchSize(100);
                long l = 150;
                stmt.setLong(1, l);
                stmt.setLong(2, l);
                stmt.setLong(3, l);
                ResultSet rs = stmt.executeQuery();
                int d = 0;
                while (rs.next()) {
                    int t = rs.getInt(1);
                    d++;
                }
                rs.close();
                assertEquals(l * l * l, d);
            }
        }
    }
}
