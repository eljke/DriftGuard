package ru.eljke.driftguard.demo.kafka.ops;

/**
 * English demo documentation.
 *
 * @param processedPoints documented value
 * @param emittedEvents documented value
 * @param failedPoints documented value
 * @param routedErrors documented value
 * @param durationMeasurements documented value
 * @param totalDurationMillis documented value
 * @param maxDurationMillis documented value
 * @param meanDurationMillis documented value
 */
public record KafkaOperationsMetrics(
        double processedPoints,
        double emittedEvents,
        double failedPoints,
        double routedErrors,
        long durationMeasurements,
        double totalDurationMillis,
        double maxDurationMillis,
        double meanDurationMillis
) {
    public static KafkaOperationsMetrics empty() {
        return new KafkaOperationsMetrics(0.0, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0);
    }
}


