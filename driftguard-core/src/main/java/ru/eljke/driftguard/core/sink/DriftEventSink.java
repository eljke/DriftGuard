package ru.eljke.driftguard.core.sink;

import ru.eljke.driftguard.core.domain.DriftEvent;

/**
 * Extension point for delivering published drift events to external systems.
 *
 * <p>Core does not depend on Kafka, JDBC, HTTP, logs or UI. A user can
 * implement a sink and connect it through a Spring bean or directly through
 * {@code DriftEventSinkListener}.</p>
 */
@FunctionalInterface
public interface DriftEventSink {
    /**
     * Accepts one published drift event.
     *
     * @param event event that passed the emission policy
     */
    void accept(DriftEvent event);
}

