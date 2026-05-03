package ru.eljke.driftguard.demo.kafka.ops;

import java.util.List;

/**
 * Операционный снимок Kafka demo и Kafka Streams topology.
 *
 * @param enabled включён ли Kafka demo режим
 * @param running выполняется ли demo playback
 * @param replay выполняется ли replay-прогон
 * @param scenario текущий сценарий
 * @param inputTopic topic входных метрик demo producer-а
 * @param outputTopic topic событий drift-а
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints сколько точек опубликовано demo producer-ом
 * @param totalPoints сколько точек должно быть опубликовано
 * @param consumedEvents сколько событий прочитал demo consumer
 * @param progressPercent прогресс producer playback в процентах
 * @param streamsApplicationId Kafka Streams application id
 * @param streamsInputTopics topic-и, из которых читает topology
 * @param streamsOutputTopic topic, куда пишет topology
 * @param runtimeStateStoreName имя Kafka Streams state store-а runtime-состояния
 * @param detectionErrorMode стратегия обработки ошибок detector-а
 * @param telemetryEnabled доступны ли Micrometer-метрики Kafka topology
 * @param metrics агрегированные метрики Kafka detection pipeline
 * @param error последняя runtime-ошибка demo-контура
 */
public record KafkaOperationsSnapshot(
        boolean enabled,
        boolean running,
        boolean replay,
        String scenario,
        String inputTopic,
        String outputTopic,
        String bootstrapServers,
        int producedPoints,
        int totalPoints,
        int consumedEvents,
        double progressPercent,
        String streamsApplicationId,
        List<String> streamsInputTopics,
        String streamsOutputTopic,
        String runtimeStateStoreName,
        String detectionErrorMode,
        boolean telemetryEnabled,
        KafkaOperationsMetrics metrics,
        String error
) {
}
