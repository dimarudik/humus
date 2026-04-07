package sdk.humus.executiontime.spring;

import dev.humus.discovery.DatabaseDiscoveryServiceGrpc;
import dev.humus.discovery.DiscoveryRequest;
import dev.humus.discovery.DiscoveryResponse;
import dev.humus.discovery.InstanceType;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@DirtiesContext
public class SpringExecutionTimePluginTest {
    @Container
    @Deprecated
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("exec_time_db")
            .withUsername("user")
            .withPassword("pass");

    private static Server grpcServer;
    private static final int GRPC_PORT = 9095;

    @Autowired
    private DataSource dataSource;

    @BeforeAll
    static void setup() throws Exception {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        grpcServer = ServerBuilder.forPort(GRPC_PORT)
                .addService(new DatabaseDiscoveryServiceGrpc.DatabaseDiscoveryServiceImplBase() {
                    @Override
                    public void getDatabaseInstance(DiscoveryRequest req, StreamObserver<DiscoveryResponse> obs) {
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

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:humus:grpc://localhost:" + GRPC_PORT + "/exec_time_db");
        registry.add("spring.datasource.driver-class-name", () -> "sdk.humus.HumusDriver");
        registry.add("spring.datasource.username", () -> "user");
        registry.add("spring.datasource.password", () -> "pass");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.datasource.type", () -> "com.zaxxer.hikari.HikariDataSource");
        registry.add("spring.datasource.hikari.dataSourceProperties.slow-query-threshold-ms", () -> "1000");
    }

    @Test
    @DisplayName("Тестируем ExecutionTimePlugin с настройкой порога в Spring")
    void testExecutionTime() throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            long start = System.currentTimeMillis();
            stmt.execute("SELECT pg_sleep(1.5)");
            long duration = System.currentTimeMillis() - start;

            assertTrue(duration >= 1500, "Query should take at least 1500ms");
        }
    }

    @SpringBootApplication
    static class TestApp {}
}
