package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Обогащает OpenAPI схемы моделей, которые приходят из core/testkit модулей.
 */
@SuppressWarnings("rawtypes")
@Configuration
public class OpenApiSchemaConfiguration {
    @Bean
    public OpenApiCustomizer driftGuardSchemaDescriptions() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            describe(schemas, "DemoRunResult", "Результат запуска demo-сценария с точками метрик, событиями и качеством детекции.", Map.of(
                    "scenario", "Технический id сценария.",
                    "title", "Человекочитаемое имя сценария.",
                    "metricPoints", "Количество сгенерированных точек метрик.",
                    "samplePoints", "Точки метрик, использованные для построения графика.",
                    "expectedDrifts", "Ожидаемые интервалы drift-а в synthetic сценарии.",
                    "events", "События drift-а, опубликованные detector-ами.",
                    "quality", "Оценка качества детекции на сценарии."
            ));
            describe(schemas, "DetectionMetrics", "Метрики качества detector-а на synthetic сценарии.", Map.of(
                    "events", "Всего опубликованных событий.",
                    "truePositiveEvents", "События внутри ожидаемых drift-интервалов.",
                    "falsePositiveEvents", "События вне ожидаемых drift-интервалов.",
                    "expectedDriftIntervals", "Количество ожидаемых drift-интервалов.",
                    "detectedDriftIntervals", "Количество интервалов, где был сигнал.",
                    "missedDriftIntervals", "Количество пропущенных ожидаемых интервалов.",
                    "detected", "Был ли найден хотя бы один ожидаемый drift.",
                    "firstDetectionDelay", "Задержка первого сигнала относительно начала drift-а.",
                    "precision", "Доля событий, попавших в ожидаемые интервалы.",
                    "recall", "Доля ожидаемых интервалов, где был сигнал."
            ));
            describe(schemas, "DriftEvent", "Публичное событие о найденном drift-е потока метрик.", Map.ofEntries(
                    Map.entry("id", "Уникальный id события."),
                    Map.entry("key", "Ключ потока метрик."),
                    Map.entry("detectedAt", "Время обнаружения события."),
                    Map.entry("direction", "Направление изменения."),
                    Map.entry("severity", "Уровень важности события."),
                    Map.entry("score", "Алгоритм-специфичная оценка drift-а."),
                    Map.entry("currentValue", "Репрезентативное текущее значение."),
                    Map.entry("baselineValue", "Репрезентативное baseline-значение."),
                    Map.entry("detector", "Имя detector definition."),
                    Map.entry("algorithm", "Имя алгоритма."),
                    Map.entry("reason", "Короткое объяснение причины события.")
            ));
            describe(schemas, "MetricPoint", "Одно наблюдаемое значение технической метрики.", Map.of(
                    "key", "Стабильная идентичность потока метрик.",
                    "timestamp", "Event timestamp точки.",
                    "value", "Числовое значение метрики.",
                    "kind", "Семантический тип метрики.",
                    "tags", "Индексируемые строковые измерения.",
                    "attributes", "Дополнительная диагностическая нагрузка."
            ));
            describe(schemas, "MetricKey", "Идентичность потока метрик.", Map.of(
                    "service", "Сервис или подсистема.",
                    "metric", "Имя метрики.",
                    "instance", "Экземпляр сервиса, pod, node или host.",
                    "operation", "Endpoint, job, consumer group или операция."
            ));
            describe(schemas, "DriftInterval", "Ожидаемый интервал drift-а в synthetic сценарии.", Map.of(
                    "start", "Начало ожидаемой деградации.",
                    "end", "Окончание ожидаемой деградации."
            ));
            describe(schemas, "DemoScenarioDescriptor", "Сценарий, доступный в demo UI и REST API.", Map.of(
                    "id", "Id сценария для endpoint-а запуска.",
                    "title", "Название сценария.",
                    "metric", "Основная метрика сценария.",
                    "description", "Краткое описание сценария."
            ));
            describe(schemas, "KafkaDemoStatus", "Состояние реального Kafka demo-контура producer -> DriftGuard Kafka Streams -> events topic.", Map.ofEntries(
                    Map.entry("enabled", "Включён ли Kafka demo режим."),
                    Map.entry("running", "Идёт ли сейчас публикация тестового потока."),
                    Map.entry("scenario", "Id запущенного сценария."),
                    Map.entry("inputTopic", "Topic входных MetricPoint."),
                    Map.entry("outputTopic", "Topic выходных DriftEvent."),
                    Map.entry("bootstrapServers", "Kafka bootstrap servers."),
                    Map.entry("producedPoints", "Количество опубликованных producer-ом точек."),
                    Map.entry("totalPoints", "Общее количество точек в сценарии."),
                    Map.entry("producers", "Состояние отдельных тестовых producer-ов."),
                    Map.entry("consumedEvents", "DriftEvent, прочитанные demo consumer-ом из output topic-а."),
                    Map.entry("samplePoints", "MetricPoint, опубликованные producer-ом и отображаемые на графике."),
                    Map.entry("error", "Последняя ошибка Kafka demo, если запуск не удался.")
            ));
            describe(schemas, "KafkaProducerStatus", "Состояние отдельного тестового producer-а в Kafka demo.", Map.of(
                    "id", "Стабильный id producer-а.",
                    "service", "Сервис, который producer имитирует.",
                    "metric", "Публикуемая метрика.",
                    "operation", "Операция или endpoint.",
                    "producedPoints", "Количество опубликованных точек.",
                    "totalPoints", "Общее количество точек producer-а.",
                    "running", "Публикует ли producer поток сейчас."
            ));
            describe(schemas, "ToolLink", "Ссылка на инструмент локального demo-стенда.", Map.of(
                    "id", "Стабильный id инструмента.",
                    "title", "Название инструмента.",
                    "url", "URL инструмента.",
                    "description", "Назначение инструмента."
            ));
            describe(schemas, "DemoConfigurationView", "Runtime-конфигурация demo и DriftGuard detector-ов для UI.", Map.of(
                    "aggressiveness", "Агрегированная оценка чувствительности detector-ов.",
                    "kafka", "Kafka-настройки demo и Kafka Streams adapter-а.",
                    "detectors", "Активные detector definitions из текущего Spring context-а."
            ));
            describe(schemas, "AggressivenessView", "Пояснение текущей чувствительности detector-ов.", Map.of(
                    "level", "Уровень чувствительности: Aggressive, Balanced или Conservative.",
                    "description", "Как этот уровень влияет на раннее обнаружение и риск ложных тревог."
            ));
            describe(schemas, "KafkaConfigurationView", "Kafka-настройки, отображаемые в разделе Configuration.", Map.of(
                    "demoEnabled", "Включены ли Kafka demo endpoints.",
                    "bootstrapServers", "Kafka bootstrap servers.",
                    "inputTopic", "Topic входных MetricPoint.",
                    "outputTopic", "Topic выходных DriftEvent.",
                    "applicationId", "Kafka Streams application id.",
                    "playbackInterval", "Интервал публикации synthetic points producer-ами."
            ));
            describe(schemas, "DetectorConfigurationView", "UI-представление одного detector definition.", Map.ofEntries(
                    Map.entry("name", "Имя detector-а."),
                    Map.entry("algorithm", "Алгоритм detector-а."),
                    Map.entry("services", "Фильтр по сервисам."),
                    Map.entry("metrics", "Фильтр по метрикам."),
                    Map.entry("warningThreshold", "Warning-порог score."),
                    Map.entry("criticalThreshold", "Critical-порог score."),
                    Map.entry("warningPValue", "Warning-порог p-value."),
                    Map.entry("criticalPValue", "Critical-порог p-value."),
                    Map.entry("warmupSamples", "Число samples для прогрева."),
                    Map.entry("emissionPolicy", "Политика публикации событий."),
                    Map.entry("sensitivity", "Оценка чувствительности detector-а.")
            ));
            describe(schemas, "EmissionPolicyView", "Политика сглаживания и подавления повторяющихся событий.", Map.of(
                    "minConsecutiveSignals", "Сколько подряд сигналов нужно перед публикацией события.",
                    "cooldown", "Минимальная пауза между событиями одного detector-а по одному stream-у."
            ));
        };
    }

    private static void describe(Map<String, Schema> schemas, String name, String description, Map<String, String> properties) {
        Schema schema = schemas.get(name);
        if (schema == null) {
            return;
        }
        schema.setDescription(description);
        Map<String, Schema> schemaProperties = schema.getProperties();
        if (schemaProperties == null) {
            return;
        }
        properties.forEach((property, propertyDescription) -> {
            Schema propertySchema = schemaProperties.get(property);
            if (propertySchema != null) {
                propertySchema.setDescription(propertyDescription);
            }
        });
    }
}
