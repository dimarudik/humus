package sdk.humus.plugins.executiontime;

import java.sql.SQLException;
import java.util.Properties;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

import static sdk.humus.plugins.executiontime.ExecutionTimeConstants.THRESHOLD_PARAM;

public class ExecutionTimePluginFactory implements ProxyPluginFactory, ProxyPlugin {

    @Override
    public ProxyPlugin create(String url, Properties info) {
        String thresholdStr = info.getProperty(THRESHOLD_PARAM);

        if (thresholdStr != null) {
            try {
                long threshold = Long.parseLong(thresholdStr);
                if (threshold > 0) {
                    return new ExecutionTimePlugin(threshold);
                }
            } catch (NumberFormatException e) {
                /* ignore invalid threshold value */
            }
        }
        return this;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> callable, Object[] args)
            throws SQLException {
        return callable.call(target, args);
    }
}
