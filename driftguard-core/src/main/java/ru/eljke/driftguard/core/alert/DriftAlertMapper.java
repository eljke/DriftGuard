package ru.eljke.driftguard.core.alert;

import ru.eljke.driftguard.core.domain.DriftEvent;

/**
 * Converts domain drift events to user-facing alerts.
 */
@FunctionalInterface
public interface DriftAlertMapper {
    /**
     * Builds an alert for one emitted drift event.
     *
     * @param event source event
     * @return alert delivered to alert sinks
     */
    DriftAlert map(DriftEvent event);
}
