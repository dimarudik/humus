package sdk.humus.core;

import java.sql.SQLException;
import java.util.List;

/**
 * Класс, управляющий последовательным выполнением цепочки плагинов.
 * <p>
 * Реализует механизм передачи управления от одного плагина к другому.
 * Если в списке больше нет плагинов, выполнение передается конечному (терминальному)
 * методу реального JDBC драйвера.
 * </p>
 *
 * <p>Порядок выполнения определяется индексом плагина в списке, который
 * предварительно сортируется драйвером согласно их весу ({@link ProxyPlugin#getOrder()}).</p>
 */
public class PluginChain {
    private final List<ProxyPlugin> plugins;
    private int currentPluginIndex = 0;

    public PluginChain(List<ProxyPlugin> plugins) {
        this.plugins = plugins;
    }

    /**
     * Запускает или продолжает выполнение цепочки плагинов.
     * <p>
     * Метод рекурсивно вызывает {@link ProxyPlugin#execute}, передавая в качестве
     * аргумента {@code next} ссылку на самого себя с инкрементированным индексом.
     * </p>
     *
     * @param <W>        Тип обертки (например, ConnectionWrapper).
     * @param <T>        Тип целевого объекта (например, PgConnection).
     * @param <R>        Тип возвращаемого значения.
     * @param wrapper    Объект-обертка, инициировавший вызов.
     * @param target     Реальный объект JDBC драйвера.
     * @param methodName Имя вызываемого метода.
     * @param terminal   Финальное действие (вызов реального метода драйвера), если цепочка пуста.
     * @param args       Аргументы вызова.
     * @return Результат выполнения всей цепочки или терминального метода.
     * @throws SQLException Если возникла ошибка на любом этапе выполнения.
     */
    public <W, T, R> R proceed(W wrapper, T target, String methodName, JdbcCallable<T, R> terminal, Object[] args)
            throws SQLException {

        if (currentPluginIndex >= plugins.size()) {
            return terminal.call(target, args);
        }

        ProxyPlugin plugin = plugins.get(currentPluginIndex++);
        return plugin.execute(
                wrapper, target, methodName, (t, a) -> proceed(wrapper, t, methodName, terminal, a), args);
    }
}
