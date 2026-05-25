package ru.eljke.driftguard.demo.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

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
 * @param speed documented value
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints documented value
 * @param totalPoints documented value
 * @param producers documented value
 * @param consumedEvents documented value
 * @param samplePoints documented value
 * @param error documented value
 */
public record KafkaDemoStatus(
        boolean enabled,
        boolean running,
        boolean replay,
        String scenario,
        String inputTopic,
        String outputTopic,
        double speed,
        String bootstrapServers,
        int producedPoints,
        int totalPoints,
        List<KafkaProducerStatus> producers,
        List<DriftEvent> consumedEvents,
        List<MetricPoint> samplePoints,
        String error
) {
}


