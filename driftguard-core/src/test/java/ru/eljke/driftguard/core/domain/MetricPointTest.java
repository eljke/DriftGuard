package ru.eljke.driftguard.core.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricPointTest {
    @Test
    void canRetimestampObservedPointWithoutChangingPayload() {
        MetricPoint original = new MetricPoint(
                MetricKey.of("checkout-service", "latency"),
                Instant.parse("2026-05-01T10:00:00Z"),
                125.0,
                MetricKind.DURATION,
                Map.of("region", "eu-central-1"),
                Map.of("sample", 12)
        );

        MetricPoint retimestamped = original.observedAt(Instant.parse("2026-05-05T17:00:00Z"));

        assertEquals(original.key(), retimestamped.key());
        assertEquals(Instant.parse("2026-05-05T17:00:00Z"), retimestamped.timestamp());
        assertEquals(original.value(), retimestamped.value());
        assertEquals(original.kind(), retimestamped.kind());
        assertEquals(original.tags(), retimestamped.tags());
        assertEquals(original.attributes(), retimestamped.attributes());
    }
}
