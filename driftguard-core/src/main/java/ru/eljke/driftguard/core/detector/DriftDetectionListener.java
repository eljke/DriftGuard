package ru.eljke.driftguard.core.detector;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Detection lifecycle listener.
 *
 * <p>Core does not depend on Micrometer, logs or Spring. External modules can
 * connect listeners for metrics, audit or debugging without changing algorithms or
 * transport-independent engine logic.</p>
 */
public interface DriftDetectionListener {
    /**
     * Called after one metric point is processed successfully.
     *
     * @param point processed metric point
     * @param events published drift events
     * @param durationNanos processing duration in nanoseconds
     */
    default void onDetectionCompleted(MetricPoint point, List<DriftEvent> events, long durationNanos) {
    }

    /**
     * Called when detection fails.
     *
     * @param point metric point that caused the failure
     * @param exception original detection pipeline error
     * @param durationNanos processing duration before the failure, in nanoseconds
     */
    default void onDetectionFailed(MetricPoint point, RuntimeException exception, long durationNanos) {
    }
}
