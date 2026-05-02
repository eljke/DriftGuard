package ru.eljke.driftguard.core.sink;

import ru.eljke.driftguard.core.domain.DriftEvent;

/**
 * Точка расширения для доставки опубликованных drift events во внешние системы.
 *
 * <p>Core не знает о Kafka, JDBC, HTTP, logs или UI. Пользователь может
 * реализовать sink и подключить его через Spring bean или напрямую через
 * {@code DriftEventSinkListener}.</p>
 */
@FunctionalInterface
public interface DriftEventSink {
    /**
     * Принимает одно опубликованное событие drift-а.
     *
     * @param event событие, прошедшее emission policy
     */
    void accept(DriftEvent event);
}
