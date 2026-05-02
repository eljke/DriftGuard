package ru.eljke.driftguard.core.config;

import java.util.List;

/**
 * Точка расширения для detector definitions, описанных кодом.
 *
 * <p>Используется, когда конфигурацию detector-а неудобно или невозможно
 * выразить через стандартные свойства {@code application.yml}, например если
 * пользовательский алгоритм имеет собственный типизированный {@link DetectorConfig}.
 * Core при этом остаётся независимым от Spring и Kafka: инфраструктурные
 * модули только собирают providers и передают их definitions в engine.</p>
 */
@FunctionalInterface
public interface DetectorDefinitionProvider {
    /**
     * Возвращает detector definitions, предоставленные этим provider-ом.
     */
    List<DetectorDefinition> definitions();
}
