package sdk.humus.spi.pluginorder;

import java.util.Properties;
import org.jspecify.annotations.NonNull;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

public class LowPriorityFactory implements ProxyPluginFactory {
    @Override
    public ProxyPlugin create(@NonNull String url, Properties info) {
        return url.contains("order-test") ? new LowPriorityPlugin() : null;
    }
}
