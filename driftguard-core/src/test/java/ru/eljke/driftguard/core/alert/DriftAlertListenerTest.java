package ru.eljke.driftguard.core.alert;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriftAlertListenerTest {
    @Test
    void mapsPublishedEventsToAlerts() {
        List<DriftAlert> alerts = new ArrayList<>();
        DriftAlertListener listener = new DriftAlertListener(new DefaultDriftAlertMapper(), List.of(alerts::add));
        DriftEvent event = event();

        listener.onDetectionCompleted(null, List.of(event), 100);

        assertEquals(1, alerts.size());
        assertEquals(event.id(), alerts.getFirst().id());
        assertEquals(event, alerts.getFirst().event());
        assertEquals(DriftSeverity.WARNING, alerts.getFirst().severity());
        assertEquals("checkout-service", alerts.getFirst().labels().get("service"));
        assertEquals("latency", alerts.getFirst().labels().get("metric"));
        assertTrue(alerts.getFirst().message().contains("Evidence:"));
        assertTrue(alerts.getFirst().message().contains("relativeChangePercent="));
        assertTrue(alerts.getFirst().message().contains("warningThreshold="));
    }

    @Test
    void isolatesSinkFailures() {
        List<DriftAlert> alerts = new ArrayList<>();
        DriftAlertListener listener = new DriftAlertListener(new DefaultDriftAlertMapper(), List.of(
                alert -> {
                    throw new IllegalStateException("sink failed");
                },
                alerts::add
        ));

        listener.onDetectionCompleted(null, List.of(event()), 100);

        assertEquals(1, alerts.size());
    }

    private static DriftEvent event() {
        return new DriftEvent(
                "event-1",
                MetricKey.builder()
                        .service("checkout-service")
                        .metric("latency")
                        .operation("POST /checkout")
                        .build(),
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T09:59:00Z"),
                Instant.parse("2026-05-01T10:00:00Z"),
                DriftDirection.UP,
                DriftSeverity.WARNING,
                2.5,
                180.0,
                90.0,
                "latency-page-hinkley",
                "page-hinkley",
                "mean shift detected",
                Map.of(),
                Map.of(
                        "relativeChangePercent", 100.0,
                        "warningThreshold", 25.0,
                        "debugOnly", "not-in-alert"
                )
        );
    }
}
