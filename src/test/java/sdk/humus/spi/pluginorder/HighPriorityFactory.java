package sdk.humus.spi.pluginorder;

import java.util.Properties;
import org.jspecify.annotations.NonNull;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

public class HighPriorityFactory implements ProxyPluginFactory {
    @Override
    public ProxyPlugin create(@NonNull String url, Properties info) {
        if (url.contains("order-test")) {
            String host = System.getProperty("test.postgres.host");
            String port = System.getProperty("test.postgres.port");
            String database = System.getProperty("test.postgres.database");
            String pgUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            return new HighPriorityPlugin(pgUrl);
        }
        return null;
    }
}
