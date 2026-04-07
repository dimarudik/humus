package sdk.humus.plugins.executionplan;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import sdk.humus.core.JdbcCallable;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ResultSetWrapper;

import static sdk.humus.plugins.executionplan.ExecutionPlanConstants.*;
import static sdk.humus.plugins.executiontime.ExecutionTimeConstants.METHOD_NAME_TEMPLATE;

public class ExecutionPlanPlugin implements ProxyPlugin {
    private static final Logger logger = Logger.getLogger(ExecutionPlanPlugin.class.getName());

    private final long thresholdMs;

    public ExecutionPlanPlugin(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public <W, T, R> R execute(W wrapper, T target, String methodName, JdbcCallable<T, R> callable, Object[] args)
            throws SQLException {
        if (methodName.startsWith(METHOD_NAME_TEMPLATE)) {
            long start = System.nanoTime();
            R result = callable.call(target, args);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            if (durationMs >= thresholdMs) {
                String sql = extractSql(args);

                if (result instanceof ResultSet rs) {
                    @SuppressWarnings("unchecked")
                    R wrappedResult = (R) new ResultSetWrapper(rs, () -> {
                        String executionPlan = captureExecutionPlan(target);
                        logger.log(Level.FINE, LOG_PLAN_TEMPLATE, new Object[] {sql, durationMs, executionPlan});
                    });
                    return wrappedResult;
                }

                String executionPlan = captureExecutionPlan(target);
                logger.log(Level.FINE, LOG_PLAN_TEMPLATE, new Object[] {sql, durationMs, executionPlan});
            }
            return result;
        }

        return callable.call(target, args);
    }

    public String captureExecutionPlan(Object target) {
        synchronized (target) {
            try {
                Connection conn = extractConnection(target);

                if (!conn.getAutoCommit()) {
                    return "[Skipped: Active transaction detected]";
                }

                try (Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(EXECUTION_PLAN_QUERY)) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to obtain execution plan ", e);
            }
        }
        return "";
    }

    private Connection extractConnection(Object target) throws SQLException {
        if (target instanceof Connection c) return c;
        if (target instanceof Statement s) return s.getConnection();
        throw new SQLException("Could not extract native Connection from " + target.getClass());
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
