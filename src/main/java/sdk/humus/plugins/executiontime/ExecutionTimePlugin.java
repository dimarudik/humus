package sdk.humus.plugins.executiontime;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;

import static sdk.humus.plugins.executiontime.ExecutionTimeConstants.*;

public class ExecutionTimePlugin implements ProxyPlugin {
    private static final Logger logger = Logger.getLogger(ExecutionTimePlugin.class.getName());
    private final long thresholdMs;

    public ExecutionTimePlugin(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> callable, Object[] args)
            throws SQLException {

        if (methodName.startsWith(METHOD_NAME_TEMPLATE)) {
            long start = System.nanoTime();
            try {
                return callable.call(target, args);
            } finally {
                long durationMs = (System.nanoTime() - start) / 1_000_000;
                if (durationMs >= thresholdMs) {
                    String sql = extractSql(args);
                    logger.log(Level.FINE, LOG_TIME_TEMPLATE, new Object[] {sql, durationMs});
                }
            }
        }

        return callable.call(target, args);
    }

    private String extractSql(Object[] args) {
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "PreparedStatement/CallableStatement";
    }

    @Override
    public int getOrder() {
        return PRIORITY;
    }
}
