package sdk.humus;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import sdk.humus.core.ConnectionWrapper;
import sdk.humus.core.ProxyPlugin;
import sdk.humus.core.ProxyPluginFactory;

import static sdk.humus.DriverConstants.PREFIX;

/**
 * Реализация JDBC-драйвера, выступающего в роли прокси-слоя.
 * <p>
 * Драйвер перехватывает URL с префиксом {@code jdbc:humus:}, загружает цепочку плагинов
 * через {@link ServiceLoader} и делегирует выполнение реальному (underlying) драйверу.
 */
public class HumusDriver implements Driver {
    private static final Logger logger = Logger.getLogger(HumusDriver.class.getName());

    static {
        try {
            DriverManager.registerDriver(new HumusDriver());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to register HumusDriver", e);
        }
    }

    /**
     * Устанавливает соединение с базой данных, проходя через цепочку плагинов.
     * <p>
     * Процесс включает:
     * 1. Загрузку плагинов через фабрики.
     * 2. Резолвинг целевого JDBC URL (например, из gRPC в реальный postgres адрес).
     * 3. Поиск оригинального драйвера для полученного URL.
     * 4. Обертывание физического соединения в {@link ConnectionWrapper}.
     *
     * @param url  исходный JDBC URL (должен начинаться с "jdbc:humus:").
     * @param info свойства подключения.
     * @return проксированное соединение или {@code null}, если URL не поддерживается.
     * @throws SQLException если не найден целевой драйвер или плагины не смогли разрешить URL.
     */
    @Override
    @Nullable
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) return null;

        List<ProxyPlugin> plugins = loadPlugins(url, info);

        String targetUrl = url;
        for (ProxyPlugin plugin : plugins) {
            String resolved = plugin.getTargetUrl(targetUrl, info);
            if (!resolved.equals(targetUrl)) {
                targetUrl = resolved;
                break;
            }
        }

        if (targetUrl.startsWith(PREFIX)) {
            throw new SQLException("No plugin was able to resolve the target database URL for: " + url);
        }

        Driver underlyingDriver = findUnderlyingDriver(targetUrl);
        Connection physicalConn = underlyingDriver.connect(targetUrl, info);

        return new ConnectionWrapper(physicalConn, plugins, url, info);
    }

    /**
     * Загружает и инициализирует доступные плагины.
     * <p>
     * Использует {@link ServiceLoader} для поиска реализаций {@link ProxyPluginFactory}.
     * Плагины сортируются согласно их приоритету (метод {@code getOrder()}).
     */
    private List<ProxyPlugin> loadPlugins(String url, Properties info) {
        List<ProxyPlugin> plugins = new ArrayList<>();
        ServiceLoader<ProxyPluginFactory> loader = ServiceLoader.load(ProxyPluginFactory.class);

        for (ProxyPluginFactory factory : loader) {
            ProxyPlugin plugin = factory.create(url, info);
            if (plugin != null) {
                plugins.add(plugin);
            }
        }

        plugins.sort(java.util.Comparator.comparingInt(ProxyPlugin::getOrder));
        plugins.forEach(plugin ->
                logger.log(Level.FINE, "Loaded plugin: " + plugin.getClass().getName()));

        return plugins;
    }

    /**
     * Ищет в {@link DriverManager} оригинальный драйвер, способный обработать разрешенный URL.
     * <p>
     * Исключает сам {@code HumusDriver} из поиска во избежание рекурсии.
     */
    private Driver findUnderlyingDriver(String url) throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (!(driver instanceof HumusDriver) && driver.acceptsURL(url)) {
                return driver;
            }
        }
        throw new SQLException("No suitable underlying driver found for " + url);
    }

    /**
     * Проверяет, начинается ли URL с поддерживаемого префикса {@code jdbc:humus:}.
     */
    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger("dev.humus");
    }
}
