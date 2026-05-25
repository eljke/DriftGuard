package ru.eljke.driftguard.demo.kafka.ops;

import java.util.List;

/**
 * English demo documentation.
 *
 * @param enabled documented value
 * @param running documented value
 * @param replay documented value
 * @param scenario documented value
 * @param inputTopic documented value
 * @param outputTopic documented value
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints documented value
 * @param totalPoints documented value
 * @param consumedEvents documented value
 * @param progressPercent documented value
 * @param streamsApplicationId Kafka Streams application id
 * @param streamsInputTopics documented value
 * @param streamsOutputTopic documented value
 * @param runtimeStateStoreName documented value
 * @param detectionErrorMode documented value
 * @param telemetryEnabled documented value
 * @param metrics documented value
 * @param error documented value
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


