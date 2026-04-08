package sdk.humus.spi.simple;

import java.sql.SQLException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;

public class BublikDiscoveryPlugin implements ProxyPlugin {
    private static final Logger log = LoggerFactory.getLogger(BublikDiscoveryPlugin.class);
    private final String mockHost;
    private final int mockPort;
    private static final int PRIORITY = 100;

    public BublikDiscoveryPlugin(String host, int port) {
        this.mockHost = host;
        this.mockPort = port;
    }

    @Override
    public String getTargetUrl(String url, Properties info) {
        String dbName = url.substring(url.lastIndexOf("/") + 1);
        log.info(
                "Discovery Test Plugin via SPI resolved URL to: jdbc:postgresql://{}:{}/{}",
                mockHost,
                mockPort,
                dbName);
        return "jdbc:postgresql://" + mockHost + ":" + mockPort + "/" + dbName;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> next, Object[] args)
            throws SQLException {
        return next.call(target, args);
    }

    @Override
    public int getOrder() {
        return PRIORITY;
    }
}
