package ru.eljke.driftguard.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.eljke.driftguard.core.alert.DriftAlert;
import ru.eljke.driftguard.core.alert.DriftAlertSink;
import ru.eljke.driftguard.core.domain.DriftSeverity;

/**
 * Default alert sink that writes drift alerts to the application log.
 *
 * <p>Applications that need Telegram, Slack, email or incident-management
 * delivery should define their own {@link DriftAlertSink} bean. The starter
 * backs off automatically.</p>
 */
public final class Slf4jDriftAlertSink implements DriftAlertSink {
    private static final Logger log = LoggerFactory.getLogger("ru.eljke.driftguard.alerts");

    @Override
    public void accept(DriftAlert alert) {
        if (alert.severity() == DriftSeverity.CRITICAL) {
            log.error("{}", alert.message());
        } else if (alert.severity() == DriftSeverity.WARNING) {
            log.warn("{}", alert.message());
        } else {
            log.info("{}", alert.message());
        }
    }
}
