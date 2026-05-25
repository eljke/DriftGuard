package ru.eljke.driftguard.demo.kafka;

/**
 * English demo documentation.
 *
 * @param id documented value
 * @param service documented value
 * @param metric documented value
 * @param operation documented value
 * @param producedPoints documented value
 * @param totalPoints documented value
 * @param running documented value
 */
public record KafkaProducerStatus(
        String id,
        String service,
        String metric,
        String operation,
        int producedPoints,
        int totalPoints,
        boolean running
) {
}


