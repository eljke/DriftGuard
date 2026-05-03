package ru.eljke.driftguard.core.state;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.detector.DetectorState;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SimpleDetectorStateCodecRegistryTest {

    @Test
    void findsCodecByAlgorithmAndStateType() {
        FakeCodec codec = new FakeCodec();
        SimpleDetectorStateCodecRegistry registry = new SimpleDetectorStateCodecRegistry(List.of(codec));

        assertSame(codec, registry.findByAlgorithm("fake").orElseThrow());
        assertSame(codec, registry.findByState(new FakeState("value")).orElseThrow());
        assertEquals(List.of("fake"), List.copyOf(registry.algorithms()));
    }

    @Test
    void fallsBackToAlgorithmWhenStateClassIsDifferent() {
        FakeCodec codec = new FakeCodec();
        SimpleDetectorStateCodecRegistry registry = new SimpleDetectorStateCodecRegistry(List.of(codec));

        assertSame(codec, registry.findByState(new OtherFakeState()).orElseThrow());
    }

    @Test
    void rejectsDuplicateAlgorithms() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleDetectorStateCodecRegistry(List.of(new FakeCodec(), new DuplicateAlgorithmCodec()))
        );

        assertTrue(error.getMessage().contains("Duplicate detector state codec for algorithm"));
    }

    @Test
    void rejectsDuplicateStateTypes() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new SimpleDetectorStateCodecRegistry(List.of(new FakeCodec(), new DuplicateStateTypeCodec()))
        );

        assertTrue(error.getMessage().contains("Duplicate detector state codec for state type"));
    }

    @Test
    void codecSerializesRuntimeSafeDetectorState() {
        FakeCodec codec = new FakeCodec();

        byte[] payload = codec.serializeState(new FakeState("abc"));

        assertEquals(new FakeState("abc"), codec.deserialize(payload));
    }

    private record FakeState(String value) implements DetectorState {
        @Override
        public String algorithm() {
            return "fake";
        }
    }

    private record OtherFakeState() implements DetectorState {
        @Override
        public String algorithm() {
            return "fake";
        }
    }

    private static class FakeCodec implements DetectorStateCodec<FakeState> {
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

    private static final class DuplicateAlgorithmCodec extends FakeCodec {
        @Override
        public Class<FakeState> stateType() {
            return FakeState.class;
        }
    }

    private static final class DuplicateStateTypeCodec extends FakeCodec {
        @Override
        public String algorithm() {
            return "other";
        }
    }
}
