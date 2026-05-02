package ru.eljke.driftguard.demo.event;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;

/**
 * Drift event вместе с demo-specific метаданными хранения.
 *
 * @param source источник события: synthetic, live или kafka
 * @param runId id конкретного demo-прогона
 * @param receivedAt время попадания события в demo-хранилище
 * @param event исходное событие DriftGuard
 */
public record DemoStoredDriftEvent(
        String source,
        String runId,
        Instant receivedAt,
        DriftEvent event
) {
    public DemoStoredDriftEvent {
        source = source == null || source.isBlank() ? "unknown" : source;
        runId = runId == null || runId.isBlank() ? "unknown" : runId;
        receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    }
}