package ru.eljke.driftguard.demo.kafka;

import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

/**
 * Снимок состояния интеграционного Kafka demo.
 *
 * @param enabled включён ли Kafka demo режим в конфигурации
 * @param running выполняется ли сейчас producer playback
 * @param scenario id запущенного сценария
 * @param inputTopic topic входных метрик
 * @param outputTopic topic событий drift-а
 * @param bootstrapServers Kafka bootstrap servers
 * @param producedPoints сколько точек уже опубликовано producer-ом
 * @param totalPoints сколько точек должно быть опубликовано всего
 * @param producers состояние отдельных тестовых producer-ов
 * @param consumedEvents события drift-а, прочитанные из output topic-а
 * @param samplePoints точки, опубликованные в Kafka и отображаемые на графике
 * @param error последняя ошибка интеграционного режима, если она была
 */
public record KafkaDemoStatus(
        boolean enabled,
        boolean running,
        String scenario,
        String inputTopic,
        String outputTopic,
        String bootstrapServers,
        int producedPoints,
        int totalPoints,
        List<KafkaProducerStatus> producers,
        List<DriftEvent> consumedEvents,
        List<MetricPoint> samplePoints,
        String error
) {
}
