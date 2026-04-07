package sdk.humus.core;

import java.sql.SQLException;

/**
 * Функциональный интерфейс для выполнения терминальных или промежуточных вызовов JDBC.
 * <p>
 * Используется в механизме цепочки плагинов {@link PluginChain} для абстрагирования
 * финального действия — вызова реального метода целевого драйвера (например, PostgreSQL).
 * </p>
 *
 * @param <T> Тип целевого объекта JDBC (например, Connection, Statement, ResultSet).
 * @param <R> Тип возвращаемого значения метода.
 */
@FunctionalInterface
public interface JdbcCallable<T, R> {

    /**
     * Выполняет вызов метода у целевого объекта с заданными аргументами.
     *
     * @param target Объект реального JDBC драйвера, у которого вызывается метод.
     * @param args   Массив аргументов, переданных в метод (могут быть модифицированы плагинами).
     * @return Результат выполнения метода.
     * @throws SQLException Если возникла ошибка на стороне JDBC драйвера.
     */
    R call(T target, Object[] args) throws SQLException;
}
