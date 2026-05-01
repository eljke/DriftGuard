package ru.eljke.driftguard.demo;

/**
 * Состояние тестового producer-а в Kafka demo.
 *
 * @param id стабильный id producer-а
 * @param service сервис, который producer имитирует
 * @param metric публикуемая метрика
 * @param operation операция или endpoint
 * @param producedPoints сколько точек уже опубликовано этим producer-ом
 * @param totalPoints сколько точек producer должен опубликовать всего
 * @param running продолжает ли producer публиковать поток
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
