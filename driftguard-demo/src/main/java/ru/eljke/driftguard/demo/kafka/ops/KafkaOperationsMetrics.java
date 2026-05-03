package ru.eljke.driftguard.demo.kafka.ops;

/**
 * Агрегированные технические метрики Kafka detection pipeline для demo UI.
 *
 * @param processedPoints сколько metric point обработала Kafka topology
 * @param emittedEvents сколько drift events создала Kafka topology
 * @param failedPoints сколько metric point завершились ошибкой detector-а
 * @param routedErrors сколько ошибок отправлено в diagnostic topic
 * @param durationMeasurements сколько измерений latency записано в Micrometer timer
 * @param totalDurationMillis суммарная длительность обработки metric point
 * @param maxDurationMillis максимальная длительность обработки одной metric point
 * @param meanDurationMillis средняя длительность обработки metric point
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
