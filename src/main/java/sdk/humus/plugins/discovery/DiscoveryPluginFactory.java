package sdk.humus.plugins.discovery;

import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

import static sdk.humus.plugins.discovery.DiscoveryConstants.URL_PATTERN;

public class DiscoveryPluginFactory implements ProxyPluginFactory, ProxyPlugin {

    @Override
    public ProxyPlugin create(String url, Properties info) {
        Matcher matcher = URL_PATTERN.matcher(url);
        if (matcher.find()) {
            String host = matcher.group(1);
            String port = matcher.group(2);
            String cluster = matcher.group(3);

            return new DiscoveryPlugin(host + ":" + port, cluster);
        }
        return this;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> callable, Object[] args)
            throws SQLException {
        return callable.call(target, args);
    }
}
