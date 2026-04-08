package sdk.humus.core;

import java.util.Properties;
import sdk.humus.HumusDriver;

/**
 * Интерфейс фабрики для создания экземпляров {@link ProxyPlugin}.
 * <p>
 * Реализации этого интерфейса должны быть зарегистрированы в файле
 * {@code META-INF/services/sdk.humus.core.ProxyPluginFactory} для автоматического
 * обнаружения механизмом Java SPI (Service Provider Interface).
 * </p>
 *
 * <p>Драйвер {@link HumusDriver} при каждом новом подключении опрашивает
 * все найденные фабрики. Каждая фабрика самостоятельно решает, нужно ли создавать
 * плагин для данного конкретного URL.</p>
 */
public interface ProxyPluginFactory {

    /**
     * Создает и инициализирует экземпляр плагина для указанного подключения.
     * <p>
     * Метод должен проанализировать переданный {@code url} и решить,
     * применим ли данный плагин к текущему соединению.
     * </p>
     *
     * @param url  JDBC URL, переданный в метод {@code connect} (например, jdbc:humus:grpc://...)
     * @param info Свойства подключения, содержащие учетные данные и пользовательские настройки.
     * @return Экземпляр {@link ProxyPlugin}, если фабрика поддерживает данный тип подключения;
     *         {@code null}, если плагин не должен быть добавлен в цепочку для этого URL.
     */
    ProxyPlugin create(String url, Properties info);
}
