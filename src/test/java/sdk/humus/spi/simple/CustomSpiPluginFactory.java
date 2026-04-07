package sdk.humus.spi.simple;

import java.util.Properties;
import org.jspecify.annotations.NonNull;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

public class CustomSpiPluginFactory implements ProxyPluginFactory {
    @Override
    public ProxyPlugin create(@NonNull String url, Properties info) {
        if (url.startsWith("jdbc:humus:custom-spi://")) {
            String host = System.getProperty("test.postgres.host", "127.0.0.1");
            int port = Integer.parseInt(System.getProperty("test.postgres.port", "5432"));

            return new CustomSpiDiscoveryPlugin(host, port);
        }
        return null;
    }
}
