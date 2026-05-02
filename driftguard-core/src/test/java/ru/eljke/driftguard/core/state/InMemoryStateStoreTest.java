package ru.eljke.driftguard.core.state;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.detector.DetectorInstanceKey;
import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.detector.EmissionState;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStateStoreTest {
    @Test
    void updatesDetectorStateAtomicallyPerKey() throws InterruptedException {
        InMemoryDetectorStateStore store = new InMemoryDetectorStateStore();
        DetectorInstanceKey key = new DetectorInstanceKey(MetricKey.of("orders", "latency"), "counting");

        runConcurrently(8, 1_000, () -> store.update(
                key,
                () -> new CountingState(0),
                state -> new CountingState(((CountingState) state).count() + 1)
        ));

        CountingState state = (CountingState) store.get(key).orElseThrow();
        assertEquals(8_000, state.count());
    }

    @Test
    void updatesEmissionStateAtomicallyPerKey() throws InterruptedException {
        InMemoryEmissionStateStore store = new InMemoryEmissionStateStore();
        DetectorInstanceKey key = new DetectorInstanceKey(MetricKey.of("orders", "latency"), "counting");

        runConcurrently(8, 1_000, () -> store.update(
                key,
                state -> new EmissionState(
                        state.consecutiveSignals() + 1,
                        state.lastEmittedAt(),
                        state.activeEpisode(),
                        state.consecutiveNormal(),
                        state.lastEmittedEvent()
                )
        ));

        EmissionState state = store.get(key).orElseThrow();
        assertEquals(8_000, state.consecutiveSignals());
    }


    @Test
    void updatesRuntimeStateAtomicallyPerKey() throws InterruptedException {
        InMemoryDetectorRuntimeStateStore store = new InMemoryDetectorRuntimeStateStore();
        DetectorInstanceKey key = new DetectorInstanceKey(MetricKey.of("orders", "latency"), "counting");

        runConcurrently(8, 1_000, () -> store.update(
                key,
                () -> DetectorRuntimeState.initial(new CountingState(0)),
                state -> state.advance(
                        new CountingState(((CountingState) state.detectorState()).count() + 1),
                        new EmissionState(
                                state.emissionState().consecutiveSignals() + 1,
                                state.emissionState().lastEmittedAt(),
                                state.emissionState().activeEpisode(),
                                state.emissionState().consecutiveNormal(),
                                state.emissionState().lastEmittedEvent()
                        )
                )
        ));

        DetectorRuntimeState state = store.get(key).orElseThrow();
        assertEquals(8_000, ((CountingState) state.detectorState()).count());
        assertEquals(8_000, state.emissionState().consecutiveSignals());
        assertEquals(8_000, state.version());
    }

    @SuppressWarnings("SameParameterValue")
    private static void runConcurrently(int threads, int iterationsPerThread, Runnable action) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int thread = 0; thread < threads; thread++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int iteration = 0; iteration < iterationsPerThread; iteration++) {
                            action.run();
                        }
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private record CountingState(int count) implements DetectorState {
        @Override
        public String algorithm() {
            return "counting";
        }
    }
}
