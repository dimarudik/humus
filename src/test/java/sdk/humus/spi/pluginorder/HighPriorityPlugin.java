package sdk.humus.spi.pluginorder;

import java.sql.SQLException;
import java.util.Properties;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;

public class HighPriorityPlugin implements ProxyPlugin {
    private final String pgUrl;
    private static final int PRIORITY = 50;

    public HighPriorityPlugin(String pgUrl) {
        this.pgUrl = pgUrl;
    }

    @Override
    public String getTargetUrl(String url, Properties info) {
        return pgUrl;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> next, Object[] args)
            throws SQLException {
        OrderTracker.CALL_ORDER.add("HighPriority");
        return next.call(target, args);
    }

    @Override
    public int getOrder() {
        return PRIORITY;
    }
}
