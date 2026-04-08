package sdk.humus.plugins.executionplan;

public abstract class ExecutionPlanConstants {
    public static final int PRIORITY = 300;
    public static final String LOG_PLAN_TEMPLATE = "SLOW QUERY DETECTED: {0}; EXECUTION TIME: {1} ms. \n{2}\n";
    public static final String THRESHOLD_PARAM = "slow-query-plan-threshold-ms";
    public static final String EXECUTION_PLAN_QUERY = "SELECT $$ " + "                                    QUERY PLAN \n"
            + "-------------------------------------------------------------------------------------- \n"
            + " Index Scan using pg_class_oid_index on pg_class  (cost=0.27..2.29 rows=1 width=265) \n"
            + "   Index Cond: (oid = '1'::oid) \n"
            + "(2 rows)$$ as plan;";
}
