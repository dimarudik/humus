package sdk.humus.bublik;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import dev.bublik.core.model.Config;
import dev.bublik.core.model.ConnectionProperty;
import dev.bublik.core.service.StorageService;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.MountableFile;
import org.yaml.snakeyaml.Yaml;

@Testcontainers
@DirtiesContext
public class Pg2PgTest {
    @Container
    @Deprecated
    public PostgreSQLContainer<?> source = new PostgreSQLContainer<>("postgres")
            .withInitScript("./bublik/source-init.sql")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("./bublik/bublik.png"), "/var/lib/postgresql/bublik.png")
            .withDatabaseName("bublik")
            .withUsername("test")
            .withPassword("test");

    @Container
    @Deprecated
    public PostgreSQLContainer<?> target = new PostgreSQLContainer<>("postgres")
            .withInitScript("./bublik/target-init.sql")
            .withDatabaseName("bublik")
            .withUsername("test")
            .withPassword("test")
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(new HostConfig()
                    .withPortBindings(new PortBinding(Ports.Binding.bindPort(5432), new ExposedPort(5432)))));

    @BeforeAll
    static void setup() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        java.util.logging.Logger.getLogger("").setLevel(java.util.logging.Level.FINEST);
    }

    @Test
    @DisplayName("Should load custom plugin via SPI")
    void testPluginLoading() throws Exception {
        Class.forName("sdk.humus.HumusDriver");

        Properties props = new Properties();
        props.setProperty("user", "test");
        props.setProperty("password", "test");
        props.setProperty("humus.slow-query-threshold-ms", "1000");

        System.setProperty("test.postgres.host", source.getHost());
        System.setProperty("test.postgres.port", String.valueOf(source.getMappedPort(5432)));

        List<Config> configs = getConfigs();
        ConnectionProperty property = connectionProperty();
        StorageService.init(property, configs, false, 50_000, "_bublik");
    }

    private static List<Config> getConfigs() throws IOException {
        ObjectMapper mapperJSON = new ObjectMapper();
        try (InputStream is = Pg2PgTest.class.getResourceAsStream("/bublik/bublik.json")) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: /bublik/bublik.json");
            }
            return List.of(mapperJSON.readValue(is, Config[].class));
        }
    }

    private static ConnectionProperty connectionProperty() throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Pg2PgTest.class.getClassLoader().getResourceAsStream("bublik/bublik.yaml")) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found in classpath: bublik/bublik.yaml");
            }
            return yaml.loadAs(in, ConnectionProperty.class);
        }
    }
}
