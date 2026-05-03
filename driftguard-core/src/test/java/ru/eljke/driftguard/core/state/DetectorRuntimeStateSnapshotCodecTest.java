package ru.eljke.driftguard.core.state;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricKey;
import ru.eljke.driftguard.core.domain.DriftSeverity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectorRuntimeStateSnapshotCodecTest {
    @Test
    void roundTripsRuntimeState() {
        DetectorRuntimeStateSnapshotCodec codec = new DetectorRuntimeStateSnapshotCodec(
                new SimpleDetectorStateCodecRegistry(List.of(new FakeStateCodec()))
        );
        Instant detectedAt = Instant.parse("2026-05-03T00:00:00Z");
        DriftEvent event = new DriftEvent(
                "event-1",
                MetricKey.of("orders", "latency"),
                detectedAt,
                detectedAt,
                detectedAt,
                DriftDirection.UP,
                DriftSeverity.WARNING,
                42.0,
                10.0,
                1.0,
                "fake-detector",
                "fake",
                "details",
                java.util.Map.of(),
                java.util.Map.of()
        );
        DetectorRuntimeState state = new DetectorRuntimeState(
                new FakeState("value"),
                new EmissionState(2, Instant.parse("2026-05-03T00:01:00Z"), true, 3, event),
                7
        );

        DetectorRuntimeStateSnapshot snapshot = codec.serialize(state);
        DetectorRuntimeState restored = codec.deserialize(snapshot);

        assertEquals(DetectorRuntimeStateSchema.CURRENT_VERSION, snapshot.schemaVersion());
        assertEquals("fake", snapshot.algorithm());
        assertArrayEquals("value".getBytes(StandardCharsets.UTF_8), snapshot.detectorStatePayload());
        assertEquals(state, restored);
    }

    @Test
    void failsWhenCodecIsMissingDuringSerialize() {
        DetectorRuntimeStateSnapshotCodec codec = new DetectorRuntimeStateSnapshotCodec(
                new SimpleDetectorStateCodecRegistry(List.of())
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> codec.serialize(DetectorRuntimeState.initial(new FakeState("value")))
        );
    }

    @Test
    void failsWhenCodecIsMissingDuringDeserialize() {
        DetectorRuntimeStateSnapshotCodec codec = new DetectorRuntimeStateSnapshotCodec(
                new SimpleDetectorStateCodecRegistry(List.of())
        );
        DetectorRuntimeStateSnapshot snapshot = new DetectorRuntimeStateSnapshot(
                DetectorRuntimeStateSchema.CURRENT_VERSION,
                "fake",
                "value".getBytes(StandardCharsets.UTF_8),
                0,
                null,
                false,
                0,
                null,
                0
        );

        assertThrows(IllegalArgumentException.class, () -> codec.deserialize(snapshot));
    }

    private record FakeState(String value) implements DetectorState {
        @Override
        public String algorithm() {
            return "fake";
        }
    }

    private static final class FakeStateCodec implements DetectorStateCodec<FakeState> {
        @Override
        public String algorithm() {
            return "fake";
        }

        @Override
        public Class<FakeState> stateType() {
            return FakeState.class;
        }

        @Override
        public byte[] serialize(FakeState state) {
            return state.value().getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public FakeState deserialize(byte[] payload) {
            return new FakeState(new String(payload, StandardCharsets.UTF_8));
        }
    }
}
