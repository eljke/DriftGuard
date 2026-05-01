# DriftGuard

DriftGuard - модульная Java-платформа для обнаружения drift-а потоковых технических метрик в распределённых системах.

Библиотека подходит для Kafka Streams, Spring Boot приложений, batch/replay экспериментов и воспроизводимых тестовых сценариев. Core-модуль содержит доменную модель и detection engine, а интеграции с инфраструктурой вынесены в отдельные модули.

## Возможности

- `driftguard-core` с domain model, detector API, engine и state store abstraction;
- `driftguard-algorithms` с алгоритмами PSI, ADWIN-style, Page-Hinkley, Kolmogorov-Smirnov и хи-квадрат;
- `driftguard-testkit` с воспроизводимыми synthetic scenarios и оценкой качества детекции;
- `driftguard-kafka` с JSON SerDes и Kafka Streams topology builder;
- `driftguard-spring-boot-starter` с auto-configuration и настройками через `application.yml`;
- `driftguard-demo` с REST API, Swagger UI, synthetic degradation scenario и quality metrics.

## Модули

| Модуль                           | Назначение                                                                              |
|----------------------------------|-----------------------------------------------------------------------------------------|
| `driftguard-core`                | Доменная модель, контракты detector-ов, `DriftDetectorEngine`, state store abstraction. |
| `driftguard-algorithms`          | Реализации алгоритмов drift detection.                                                  |
| `driftguard-kafka`               | Kafka JSON SerDes и Kafka Streams topology.                                             |
| `driftguard-spring-boot-starter` | Spring Boot auto-configuration для подключения DriftGuard через YAML.                   |
| `driftguard-testkit`             | Synthetic metric scenarios, expected drift intervals, detection quality evaluation.     |
| `driftguard-demo`                | Demo-приложение с REST API и OpenAPI-документацией.                                    |

## Как Это Работает

```text
Metric source
  -> MetricPoint
  -> DriftDetectorEngine
  -> DetectorAlgorithm
  -> DetectorStateStore
  -> DriftEvent
```

`MetricPoint` - одна точка технической метрики.  
`MetricKey` определяет поток метрики: service, metric, instance, operation.  
`DetectorDefinition` связывает имя detector-а, algorithm config и selector метрик.  
`DriftDetectorEngine` применяет подходящие detector-ы, обновляет состояние и возвращает `DriftEvent`, если drift обнаружен.

## Алгоритмы

| Алгоритм       | Когда использовать                                                                                                                  |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `page-hinkley` | Быстрый online-сигнал о сдвиге среднего: latency, processing time, queue size, CPU, memory.                                         |
| `adwin`        | Adaptive-window mean shift detector для потоковой обработки. Реализован ADWIN-style detector на sliding window и Hoeffding bound. |
| `psi`          | Сравнение baseline/current распределений через Population Stability Index.                                                          |
| `ks`           | Двухвыборочный критерий Колмогорова-Смирнова для непрерывных распределений.                                                         |
| `chi-square`   | Binned хи-квадрат тест для распределений, которые можно осмысленно разбить на bucket-ы.                                             |

## Быстрая Проверка

```bash
mvn test
```

Ожидаемый результат:

```text
BUILD SUCCESS
```

## Запуск Demo

```bash
mvn install -DskipTests
mvn -f driftguard-demo/pom.xml spring-boot:run
```

После старта доступны endpoints:

```text
GET  http://localhost:8080/api/demo
GET  http://localhost:8080/api/demo/events
GET  http://localhost:8080/api/demo/quality
GET  http://localhost:8080/api/demo/help
POST http://localhost:8080/api/demo/run
GET  http://localhost:8080/v3/api-docs
GET  http://localhost:8080/swagger-ui.html
```

Demo запускает synthetic latency degradation scenario:

- первые точки имитируют стабильную latency около 100 ms;
- затем происходит step degradation примерно до 260 ms;
- DriftGuard обрабатывает поток через `DriftDetectorEngine`;
- REST API показывает drift events и quality metrics.

## Использование Core Без Spring И Kafka

```java
DetectorRegistry registry = DefaultAlgorithms.registry();
DetectorStateStore stateStore = new InMemoryDetectorStateStore();

List<DetectorDefinition> definitions = List.of(
        new DetectorDefinition(
                "latency-page-hinkley",
                new PageHinkleyConfig(20, 0.1, 25.0, 50.0, 0.05),
                key -> key.metric().equals("latency")
        )
);

DriftDetectorEngine engine = new DriftDetectorEngine(registry, stateStore, definitions);

MetricPoint point = MetricPoint.gauge(
        new MetricKey("checkout-service", "latency", "instance-1", "POST /checkout"),
        Instant.now(),
        250.0
);

List<DriftEvent> events = engine.detect(point);
```

Такой режим подходит для unit tests, offline replay, embedded usage и custom ingestion.

## Использование В Spring Boot

Подключите starter-модуль и настройте detector-ы в `application.yml`:

```yaml
driftguard:
  enabled: true
  detectors:
    - name: latency-page-hinkley
      algorithm: page-hinkley
      metrics: [latency]
      warmup-samples: 20
      delta: 0.1
      warning-threshold: 25.0
      critical-threshold: 50.0
      alpha: 0.05
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 10s

    - name: latency-ks
      algorithm: ks
      metrics: [latency]
      baseline-window-size: 40
      current-window-size: 40
      warning-p-value: 0.05
      critical-p-value: 0.01
      emission-policy:
        min-consecutive-signals: 1
        cooldown: 30s
```

Starter автоматически создаёт:

- `DetectorRegistry`;
- `DetectorStateStore`;
- `List<DetectorDefinition>`;
- `DriftDetectorEngine`.

В приложении можно просто внедрить engine:

```java
@Service
public class MetricConsumer {
    private final DriftDetectorEngine engine;

    public MetricConsumer(DriftDetectorEngine engine) {
        this.engine = engine;
    }

    public List<DriftEvent> onMetric(MetricPoint point) {
        return engine.detect(point);
    }
}
```

## Использование С Kafka Streams

`driftguard-kafka` предоставляет:

- `JsonSerde<MetricPoint>`;
- `JsonSerde<DriftEvent>`;
- `KafkaDriftGuardTopologyBuilder`.

Пример:

```java
ObjectMapper mapper = DriftGuardObjectMapper.create();
DriftDetectorEngine engine = ...

KafkaDriftGuardTopologyConfig config = new KafkaDriftGuardTopologyConfig(
        List.of("technical-metrics"),
        "drift-events"
);

Topology topology = new KafkaDriftGuardTopologyBuilder(mapper, engine).build(config);
KafkaStreams streams = new KafkaStreams(topology, streamsProperties);
streams.start();
```

Входной topic должен содержать JSON-представление `MetricPoint`, выходной topic получает JSON `DriftEvent`.

## Использование Testkit

Testkit нужен для воспроизводимых экспериментов и проверки алгоритмов.

```java
MetricScenario scenario = new StepDriftScenario(
        "latency-step-degradation",
        ScenarioConfig.latency("checkout-service", "POST /checkout", 160),
        80,
        100.0,
        260.0,
        4.0
);

List<MetricPoint> points = scenario.generate();
List<DriftEvent> events = new ArrayList<>();

for (MetricPoint point : points) {
    events.addAll(engine.detect(point));
}

DetectionMetrics quality = DetectionEvaluator.evaluate(scenario, events);
```

Доступные сценарии:

- `StableNoiseScenario` - стабильный поток с шумом;
- `StepDriftScenario` - резкая деградация;
- `GradualDriftScenario` - плавный drift, например memory leak.

## Архитектура

- Core не зависит от Spring, Kafka, Jackson и Micrometer.
- Алгоритмы зависят только от core.
- Kafka и Spring являются adapter-слоями.
- Имена topic-ов, detector-ы, окна и пороги задаются конфигурацией.
- Новый алгоритм добавляется через `DetectorAlgorithm<C, S>`.
- Состояние detector-а должно храниться через `DetectorStateStore`.

## Используемые Библиотеки

- Lombok применяется в `driftguard-spring-boot-starter` для configuration properties, где он убирает однотипные getters/setters.
- Apache Commons Math применяется в `driftguard-algorithms` для статистических тестов KS и хи-квадрат.
- Jackson применяется в `driftguard-kafka` для JSON SerDes.
- Springdoc OpenAPI применяется в `driftguard-demo` для Swagger UI и `/v3/api-docs`.

## Как Добавить Новый Алгоритм

1. Создать immutable config, реализующий `DetectorConfig`.
2. Создать state, реализующий `DetectorState`.
3. Реализовать `DetectorAlgorithm<C, S>`.
4. Добавить алгоритм в `DefaultAlgorithms`.
5. Добавить tests на stable stream, drift stream и шум.
6. При необходимости добавить mapping в `DetectorDefinitionFactory` для Spring Boot starter.

## Статус Kafka-Модуля

Kafka-модуль содержит JSON SerDes для `MetricPoint` и `DriftEvent`, а также `KafkaDriftGuardTopologyBuilder`. Текущая topology читает `MetricPoint` из input topic-ов, вызывает `DriftDetectorEngine` и пишет `DriftEvent` в output topic.
