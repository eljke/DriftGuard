package ru.eljke.driftguard.demo.event;

import ru.eljke.driftguard.core.domain.DriftEvent;

import java.time.Instant;

/**
 * English demo documentation.
 *
 * @param source documented value
 * @param runId documented value
 * @param receivedAt documented value
 * @param event documented value
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

