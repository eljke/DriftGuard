package ru.eljke.driftguard.core.alert;

/**
 * Extension point for delivering drift alerts to external systems.
 *
 * <p>Implement this interface to send alerts to Telegram, Slack, email,
 * incident-management systems or an internal audit service. Sink failures are
 * isolated by {@link DriftAlertListener} and do not break detection.</p>
 */
@FunctionalInterface
public interface DriftAlertSink {
    /**
     * Delivers one alert.
     *
     * @param alert alert generated from a public drift event
     */
    void accept(DriftAlert alert);
}
