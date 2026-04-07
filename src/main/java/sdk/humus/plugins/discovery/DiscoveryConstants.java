package sdk.humus.plugins.discovery;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public abstract class DiscoveryConstants {
    public static final int PRIORITY = 100;
    public static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(5);
    public static final Pattern URL_PATTERN = Pattern.compile("jdbc:humus:grpc://([^:/]+):(\\d+)/(.+)");
}
