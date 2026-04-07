package sdk.humus.plugins.executiontime;

public abstract class ExecutionTimeConstants {
    public static final int PRIORITY = 200;
    public static final String LOG_TIME_TEMPLATE = "SLOW QUERY DETECTED: {0}; EXECUTION TIME: {1} ms.";
    public static final String METHOD_NAME_TEMPLATE = "execute";
    public static final String THRESHOLD_PARAM = "slow-query-threshold-ms";
}
