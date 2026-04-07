package sdk.humus.spi.pluginorder;

import java.sql.SQLException;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;

public class LowPriorityPlugin implements ProxyPlugin {
    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> next, Object[] args)
            throws SQLException {
        OrderTracker.CALL_ORDER.add("LowPriority");
        return next.call(target, args);
    }

    @Override
    public int getOrder() {
        return 900;
    }
}
