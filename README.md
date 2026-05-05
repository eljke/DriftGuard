# DriftGuard

DriftGuard — модульная Java-платформа для обнаружения drift-а потоковых технических метрик в распределённых системах реального времени.

Проект спроектирован как подключаемая библиотека и набор integration-модулей, а не как монолитное demo-приложение. `driftguard-core` содержит доменную модель и detection engine, а интеграции с Kafka, Spring Boot, testkit и demo UI вынесены в отдельные модули.

## Возможности

- `driftguard-core` с domain model, detector API, engine, registry и state store abstraction;
- `driftguard-algorithms` с алгоритмами PSI, ADWIN-style, Page-Hinkley, Kolmogorov-Smirnov и chi-square;
- `driftguard-testkit` с воспроизводимыми synthetic scenarios, expected drift intervals, benchmark runner и метриками качества;
- `driftguard-kafka` с JSON SerDes и Kafka Streams topology builder;
- `driftguard-spring-boot-starter` с auto-configuration и настройками через `application.yml`;
- `driftguard-demo` с React UI, REST API, Swagger UI, synthetic scenarios, Kafka integration demo, configuration view, drift incidents и quality metrics.

## Модули

| Модуль                           | Назначение                                                                              |
|----------------------------------|-----------------------------------------------------------------------------------------|
| `driftguard-core`                | Доменная модель, контракты detector-ов, `DriftDetectorEngine`, state store abstraction. |
| `driftguard-algorithms`          | Реализации встроенных алгоритмов drift detection.                                       |
| `driftguard-kafka`               | Kafka JSON SerDes и Kafka Streams topology.                                             |
| `driftguard-spring-boot-starter` | Spring Boot auto-configuration для подключения DriftGuard через YAML.                   |
| `driftguard-testkit`             | Synthetic metric scenarios, expected drift intervals, detection quality evaluation.     |
| `driftguard-demo`                | Demo-приложение с UI, REST API, OpenAPI, Kafka integration demo и benchmark view.       |

## Как это работает

```text
Metric source
  -> MetricPoint
  -> DriftDetectorEngine
  -> DetectorAlgorithm
  -> DetectorStateStore
  -> DriftEvent
  -> DriftEventSink
```

`MetricPoint` — одна точка технической метрики.

`MetricKey` определяет поток метрики:

```text
service
metric
instance
operation
```

`DetectorDefinition` связывает:

```text
name
algorithm config
metric selector
emission policy
```

`DriftDetectorEngine` применяет подходящие detector-ы, обновляет состояние и возвращает `DriftEvent`, если drift обнаружен.

## Integration contracts

DriftGuard intentionally keeps the integration surface small: consumers send `MetricPoint`, configure detector definitions and receive `DriftEvent`.

### MetricPoint input

`MetricPoint` is the stable input contract for core, Spring Boot and Kafka integrations.

```json
{
  "key": {
    "service": "checkout-service",
    "metric": "latency",
    "instance": "instance-1",
    "operation": "POST /checkout"
  },
  "timestamp": "2026-05-05T16:00:00Z",
  "value": 245.7,
  "kind": "DURATION",
  "tags": {
    "region": "eu-central-1"
  },
  "attributes": {}
}
```

Required fields are `key.service`, `key.metric`, `timestamp`, `value` and `kind`. `instance`, `operation`, `tags` and `attributes` are optional but useful for routing, filtering and diagnostics.

### DriftEvent output

`DriftEvent` is the stable output contract for REST API, Kafka output topic, sinks and UI.

```json
{
  "phase": "STARTED",
  "direction": "UP",
  "severity": "CRITICAL",
  "score": 72.3,
  "currentValue": 260.0,
  "baselineValue": 101.4,
  "detector": "latency-page-hinkley",
  "algorithm": "page-hinkley",
  "reason": "Page-Hinkley cumulative deviation crossed threshold",
  "details": {
    "relativeChangePercent": 156.4,
    "criticalThreshold": 50.0
  }
}
```

`phase` describes the lifecycle of one drift episode:

```text
STARTED   first public event for a drift episode
ONGOING   repeated signal after emission cooldown
RECOVERED detector observed enough normal points near baseline
```

`severity` matters for `STARTED` and `ONGOING`. `RECOVERED` is an informational lifecycle event, so UI should primarily display the phase rather than treating it as a warning or critical alert.

### Kafka topics

The Kafka integration expects JSON `MetricPoint` messages on configured `driftguard.kafka.input-topics` and publishes JSON `DriftEvent` messages to `driftguard.kafka.output-topic`.

```yaml
driftguard:
  kafka:
    enabled: true
    input-topics: [technical-metrics]
    output-topic: drift-events
```

Kafka message keys should identify the metric stream when possible, for example `checkout-service|latency|POST /checkout`. DriftGuard can still deserialize and process value-only JSON messages, but stable keys make partitioning and debugging easier.

### Demo scenario request

Demo REST endpoints accept optional scenario generation parameters for reproducible experiments:

```json
{
  "samples": 160,
  "baselineValue": 100.0,
  "driftValue": 260.0,
  "noiseStdDev": 4.0,
  "driftStartPercent": 50,
  "spikeLengthPercent": 20
}
```

The same fields are supported by `POST /api/demo/run/{scenario}`, `POST /api/demo/live/{scenario}` and `POST /api/demo/kafka/replay`. `samples` means number of generated `MetricPoint` objects; in Kafka replay this becomes the number of Kafka messages per producer stream. Omitted values use scenario defaults.

## Алгоритмы

| Алгоритм       | Когда использовать                                                                                                                |
|----------------|-----------------------------------------------------------------------------------------------------------------------------------|
| `page-hinkley` | Быстрый online-сигнал о сдвиге среднего: latency, processing time, queue size, CPU, memory.                                       |
| `adwin`        | Adaptive-window mean shift detector для потоковой обработки. Реализован ADWIN-style detector на sliding window и Hoeffding bound. |
| `psi`          | Сравнение baseline/current распределений через Population Stability Index.                                                        |
| `ks`           | Двухвыборочный критерий Колмогорова-Смирнова для непрерывных распределений.                                                       |
| `chi-square`   | Binned chi-square test для распределений, которые можно осмысленно разбить на bucket-ы.                                           |

## Быстрая проверка

```bash
mvn test
```

Kafka end-to-end тест использует Testcontainers и запускает временный Kafka broker. Если Docker недоступен, этот тест пропускается.

Ожидаемый результат:

```text
BUILD SUCCESS
```

## Запуск demo через Docker Compose

```bash
docker compose up --build
```

Compose поднимает:

- Demo UI: `http://localhost:8080`;
- Kafka: `localhost:9092`;
- Kafka UI: `http://localhost:8090`;
- Prometheus: `http://localhost:9090`;
- Grafana: `http://localhost:3000` (`admin` / `admin`).

Prometheus собирает метрики demo-приложения с `/actuator/prometheus` на `8080` и `8081`.

Demo публикует counters:

```text
driftguard.demo.scenario.runs
driftguard.demo.metric.points
driftguard.demo.drift.events
```

После старта доступны endpoints:

```text
GET  http://localhost:8080/
GET  http://localhost:8080/api/demo
GET  http://localhost:8080/api/demo/events
GET  http://localhost:8080/api/demo/events/stored
POST http://localhost:8080/api/demo/events/clear
GET  http://localhost:8080/api/demo/quality
GET  http://localhost:8080/api/demo/benchmark
GET  http://localhost:8080/api/demo/benchmark/profiles
GET  http://localhost:8080/api/demo/scenarios
GET  http://localhost:8080/api/demo/capabilities
GET  http://localhost:8080/api/demo/help
POST http://localhost:8080/api/demo/run
POST http://localhost:8080/api/demo/run/{scenario}
POST http://localhost:8080/api/demo/live/{scenario}
POST http://localhost:8080/api/demo/live/stop
GET  http://localhost:8080/api/demo/kafka
GET  http://localhost:8080/api/demo/kafka/operations
POST http://localhost:8080/api/demo/kafka/start/{scenario}
POST http://localhost:8080/api/demo/kafka/replay
POST http://localhost:8080/api/demo/kafka/stop
GET  http://localhost:8080/api/demo/tools
GET  http://localhost:8080/api/demo/configuration
POST http://localhost:8080/api/demo/configuration/profile/{profile}
GET  http://localhost:8080/v3/api-docs
GET  http://localhost:8080/swagger-ui.html
```

Demo UI позволяет запускать synthetic scenarios:

- `latency-step` — резкий рост latency;
- `error-rate-spike` — кратковременный всплеск error rate;
- `throughput-drop` — падение throughput;
- `queue-growth` — плавный рост queue size;
- `seasonal-latency` — сезонная latency без ожидаемого drift-а;
- `microservices-system` — несколько сервисов одновременно публикуют разные метрики.

Dashboard показывает:

```text
overview;
synthetic scenarios;
Kafka scenarios;
Kafka replay;
runtime detector profile;
registered algorithms;
metric streams;
drift events;
active/recovered incidents;
synthetic benchmark;
demo tools links.
```

Время в UI форматируется в таймзоне `Europe/Moscow`.

Instant-режим обрабатывает весь сценарий сразу. Live-режим проигрывает поток постепенно и показывает прогресс обработки.

Frontend demo реализован на React, TypeScript, TanStack Query и Apache ECharts. Maven собирает frontend автоматически при сборке `driftguard-demo`, поэтому отдельный ручной build для запуска через Spring Boot не нужен.

## Kafka integration demo

Раздел `Kafka Demo` запускает реальный контур:

```text
test producers
  -> driftguard.demo.metrics
  -> Kafka Streams topology
  -> driftguard.demo.drift-events
  -> demo consumer
  -> UI
```

В этом режиме demo producer-ы публикуют `MetricPoint` в Kafka, topology запускается через `driftguard-spring-boot-starter` и использует `driftguard-kafka`, а demo consumer читает `DriftEvent` из output topic-а. Сообщения и consumer groups можно смотреть в Kafka UI.

Сценарий `microservices-system` запускает несколько producer-ов:

- `checkout-service` публикует latency и throughput;
- `payment-service` публикует error rate;
- `orders-worker` публикует queue size.

Replay mode позволяет переигрывать Kafka scenario с выбранной скоростью и сбросом runtime detector state. Это удобно для воспроизводимого сравнения профилей и алгоритмов.

Пример запроса:

```json
{
  "scenario": "latency-step",
  "speed": 2.0,
  "resetState": true,
  "profile": "BALANCED"
}
```

## Локальная frontend-разработка

```bash
cd driftguard-demo/src/main/frontend
npm ci
npm run dev
```

Vite dev server проксирует `/api`, `/actuator`, `/v3` и Swagger-запросы на `localhost:8080`.

## Использование core без Spring и Kafka

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

## Использование в Spring Boot

Подключите starter-модуль и настройте detector-ы в `application.yml`.

Минимальный пример:

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
        recovery-consecutive-normal: 3
```

Starter автоматически создаёт:

- `DetectorRegistry`;
- `DetectorStateStore`;
- `List<DetectorDefinition>`;
- `DriftDetectorEngine`;
- `DriftDetectionListener` для Micrometer при наличии `MeterRegistry`;
- `DriftEventSinkListener`, если есть пользовательские `DriftEventSink` bean-ы.

Если включить `driftguard.kafka.enabled`, starter также создаёт Kafka Streams topology для обработки `MetricPoint` из Kafka:

```yaml
driftguard:
  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    application-id: driftguard-prod
    input-topics: [technical-metrics]
    output-topic: drift-events
    state-dir: ./target/kafka-streams-state
```

Topology запускается вместе со Spring context-ом и останавливается при shutdown. Для тестов или ручного запуска можно задать:

```yaml
driftguard:
  kafka:
    auto-startup: false
```

## Micrometer metrics

Если в Spring Boot приложении есть `MeterRegistry`, starter автоматически подключает Micrometer listener для detection pipeline.

Отключение:

```yaml
driftguard:
  metrics:
    enabled: false
```

Публикуемые метрики:

```text
driftguard.detection.points
  Количество обработанных MetricPoint.
  Tags: service, metric.

driftguard.detection.duration
  Длительность обработки одной MetricPoint.
  Tags: service, metric.

driftguard.detection.events
  Количество опубликованных DriftEvent.
  Tags: service, metric, detector, algorithm, severity, phase.

driftguard.detection.errors
  Количество ошибок detection pipeline.
  Tags: service, metric, exception.
```

## Конфигурация detector-ов

Detector definition в `application.yml` описывает:

- имя detector-а;
- флаг включения;
- алгоритм;
- selector потоков метрик;
- параметры алгоритма;
- emission policy.

Полный пример:

```yaml
driftguard:
  enabled: true

  kafka:
    enabled: true
    bootstrap-servers: localhost:9092
    application-id: driftguard-demo-streams
    input-topics: [driftguard.demo.metrics]
    output-topic: driftguard.demo.drift-events
    auto-startup: false
    state-dir: ./target/driftguard-demo-kafka-state

  detectors-enabled: true
  detectors:
    - name: latency-page-hinkley
      enabled: true
      algorithm: page-hinkley
      services: [checkout-service]
      metrics: [latency]
      operations: ["POST /checkout"]
      page-hinkley:
        warmup-samples: 20
        delta: 0.1
        warning-threshold: 25.0
        critical-threshold: 50.0
        alpha: 0.05
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 10s
        recovery-consecutive-normal: 3

    - name: error-rate-page-hinkley
      enabled: true
      algorithm: page-hinkley
      metrics: [error-rate]
      page-hinkley:
        warmup-samples: 20
        delta: 0.001
        warning-threshold: 0.03
        critical-threshold: 0.08
        alpha: 0.05
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 10s
        recovery-consecutive-normal: 3

    - name: throughput-ks
      enabled: true
      algorithm: ks
      metrics: [throughput]
      ks:
        baseline-window: 35
        current-window: 25
        warning-p-value: 0.04
        critical-p-value: 0.01
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 10s
        recovery-consecutive-normal: 3

    - name: queue-size-page-hinkley
      enabled: true
      algorithm: page-hinkley
      metrics: [queue-size]
      page-hinkley:
        warmup-samples: 20
        delta: 0.1
        warning-threshold: 25.0
        critical-threshold: 60.0
        alpha: 0.05
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 10s
        recovery-consecutive-normal: 3
```

Selector-поля:

```text
services    фильтр по MetricKey.service
metrics     фильтр по MetricKey.metric
operations  фильтр по MetricKey.operation
instances   фильтр по MetricKey.instance
```

Пустой список или отсутствие поля означает `any`.

Например:

```yaml
metrics: [latency]
```

означает: применять detector ко всем сервисам и операциям, но только к метрике `latency`.

А:

```yaml
services: [checkout-service]
metrics: [latency]
operations: ["POST /checkout"]
```

означает: применять detector только к latency checkout endpoint-а.

## Emission policy

Алгоритм может давать внутренние сигналы часто. `emission-policy` управляет тем, какие из этих сигналов становятся публичными `DriftEvent`.

```yaml
emission-policy:
  min-consecutive-signals: 2
  cooldown: 45s
  recovery-consecutive-normal: 3
```

Поля:

```text
min-consecutive-signals     сколько подряд drift-сигналов нужно перед событием
cooldown                    минимальная пауза между публичными событиями
recovery-consecutive-normal сколько нормальных точек нужно для закрытия episode
```

Это снижает event spam: один скачок latency должен выглядеть как один drift episode, а не как десятки одинаковых `CRITICAL`.

## Настройки `application.yml`

| Property                                      | Назначение                                                                            |
|-----------------------------------------------|---------------------------------------------------------------------------------------|
| `driftguard.enabled`                          | Включает auto-configuration DriftGuard.                                               |
| `driftguard.detectors-enabled`                | Включает detector definitions, описанные через `application.yml`.                     |
| `driftguard.metrics.enabled`                  | Включает Micrometer listener для runtime-метрик detection pipeline.                   |
| `driftguard.detectors[].enabled`              | Включает или выключает отдельный detector без удаления конфигурации.                  |
| `driftguard.detectors[].name`                 | Имя detector-а в событиях и state key.                                                |
| `driftguard.detectors[].algorithm`            | Алгоритм: `page-hinkley`, `adwin`, `psi`, `ks`, `chi-square`.                         |
| `driftguard.detectors[].services`             | Фильтр по `MetricKey.service`; пусто означает любые сервисы.                          |
| `driftguard.detectors[].metrics`              | Фильтр по `MetricKey.metric`; пусто означает любые метрики.                           |
| `driftguard.detectors[].operations`           | Фильтр по `MetricKey.operation`; пусто означает любые операции.                       |
| `driftguard.detectors[].instances`            | Фильтр по `MetricKey.instance`; пусто означает любые instance id.                     |
| `baseline-window-size`                        | Размер baseline-окна для `psi`, `ks`, `chi-square`.                                   |
| `current-window-size`                         | Размер current-окна для `psi`, `ks`, `chi-square`.                                    |
| `window-size`                                 | Размер окна для `adwin`.                                                              |
| `min-sub-window-size`                         | Минимальная часть окна при поиске разреза в `adwin`.                                  |
| `warmup-samples`                              | Число samples для прогрева `page-hinkley`.                                            |
| `buckets`                                     | Количество bucket-ов для `psi` и `chi-square`.                                        |
| `warning-threshold` / `critical-threshold`    | Пороги для `psi` и `page-hinkley`.                                                    |
| `warning-p-value` / `critical-p-value`        | P-value границы для `ks` и `chi-square`.                                              |
| `delta`                                       | Sensitivity/confidence параметр для `adwin` и `page-hinkley`.                         |
| `alpha`                                       | Скорость адаптации среднего в `page-hinkley`.                                         |
| `epsilon`                                     | Сглаживание bucket-ов в `psi`.                                                        |
| `critical-multiplier`                         | Множитель score для critical-события в `adwin`.                                       |
| `min-expected-count`                          | Минимальная ожидаемая частота bucket-а в `chi-square`.                                |
| `emission-policy.min-consecutive-signals`     | Сколько подряд сигналов нужно до публикации `DriftEvent`.                             |
| `emission-policy.cooldown`                    | Минимальная пауза между опубликованными событиями одного detector-а по одному потоку. |
| `emission-policy.recovery-consecutive-normal` | Сколько нормальных точек нужно для закрытия drift episode.                            |
| `driftguard.kafka.enabled`                    | Включает Kafka Streams topology DriftGuard.                                           |
| `driftguard.kafka.bootstrap-servers`          | Kafka bootstrap servers.                                                              |
| `driftguard.kafka.application-id`             | Kafka Streams application id.                                                         |
| `driftguard.kafka.input-topics`               | Topic-и с JSON `MetricPoint`.                                                         |
| `driftguard.kafka.output-topic`               | Topic для JSON `DriftEvent`.                                                          |
| `driftguard.kafka.state-dir`                  | Локальная директория state store-ов Kafka Streams.                                    |
| `driftguard.kafka.auto-startup`               | Автоматически запускать topology при старте Spring context-а.                         |

## Использование engine в приложении

В приложении можно внедрить engine напрямую:

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

## Drift event sinks

Если приложению нужно доставлять drift events в собственное хранилище или внешний канал, можно реализовать `DriftEventSink`.

```java
@Component
class JdbcDriftEventSink implements DriftEventSink {
    private final DriftEventRepository repository;

    JdbcDriftEventSink(DriftEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void accept(DriftEvent event) {
        repository.save(event);
    }
}
```

Spring Boot starter автоматически соберёт все `DriftEventSink` bean-ы и подключит их к `DriftDetectorEngine` через listener.

Sink получает только события, прошедшие emission policy:

```text
algorithm signal
  -> emission policy
  -> public DriftEvent
  -> DriftEventSink
```

Это значит, что sink не будет получать внутренние шумные сигналы detector-а, если они были подавлены `min-consecutive-signals`, `cooldown` или активным drift episode.

Примеры sink-ов:

```text
JDBC/PostgreSQL sink
HTTP webhook sink
Kafka sink
log sink
in-memory UI sink
custom audit sink
```

Core остаётся независимым от конкретной доставки. Для использования без Spring можно подключить sink вручную:

```java
DriftDetectionListener listener = new DriftEventSinkListener(List.of(mySink));
```

## Использование с Kafka Streams

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

## Kafka payload model

Входной поток Kafka содержит `MetricPoint`.

Минимальный JSON:

```json
{
  "key": {
    "service": "checkout-service",
    "metric": "latency",
    "instance": "checkout-1",
    "operation": "POST /checkout"
  },
  "timestamp": "2026-05-02T10:00:00Z",
  "value": 245.7,
  "kind": "DURATION",
  "unit": "ms",
  "tags": {
    "env": "demo"
  }
}
```

Выходной поток Kafka содержит `DriftEvent`.

Событие включает:

```text
key              поток метрики, где найден drift
detectedAt       время обнаружения
phase            STARTED / ONGOING / RECOVERED
direction        UP / DOWN / DISTRIBUTION / UNKNOWN
severity         INFO / WARNING / CRITICAL
score            algorithm-specific оценка drift-а
currentValue     текущее значение или summary current window
baselineValue    baseline значение или summary baseline window
detector         имя detector definition
algorithm        имя алгоритма
reason           человекочитаемая причина
tags             tags исходной метрики
details          расширяемые algorithm-specific детали
```

## Drift episode lifecycle

Публичные события имеют фазу:

```text
STARTED    первый опубликованный event episode-а
ONGOING    фаза зарезервирована для последующих обновлений episode-а
RECOVERED  событие восстановления после нормальных наблюдений
```

Текущее MVP в основном публикует `STARTED` и `RECOVERED`. Это уже позволяет UI показывать active/recovered incidents. `ONGOING` оставлен как расширение для будущего режима incident updates.

## Использование testkit

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

- `StableNoiseScenario` — стабильный поток с шумом;
- `StepDriftScenario` — резкая деградация;
- `GradualDriftScenario` — плавный drift, например memory leak;
- `PulseSpikeScenario` — кратковременный spike error rate или latency;
- `ThroughputDropScenario` — падение throughput;
- `SeasonalNoiseScenario` — стабильная сезонность без ожидаемого drift-а.

`DetectionMetrics` показывает число событий, true/false positive events, число обнаруженных и пропущенных drift-интервалов, precision, recall и задержку первого обнаружения.

## Synthetic benchmark

`driftguard-testkit` позволяет проверять detector-ы на воспроизводимых synthetic scenarios:

```java
DetectionBenchmarkResult result = DetectionBenchmarkRunner.runScenario(
        "latency-step",
        scenario,
        engine::detect
);
```

Для нескольких сценариев:

```java
DetectionBenchmarkReport report = DetectionBenchmarkRunner.report("BALANCED", results);
```

Benchmark считает:

```text
detected scenarios
true positive events
false positive events
missed intervals
precision
recall
mean first detection delay
```

Это нужно, чтобы сравнивать detector profiles не только визуально на графике, но и количественно.

## Как добавить новый алгоритм

1. Создать immutable config, реализующий `DetectorConfig`.
2. Создать state, реализующий `DetectorState`.
3. Реализовать `DetectorAlgorithm<C, S>`.
4. Добавить tests на stable stream, drift stream и шум.

### Core-only подключение

Если DriftGuard используется без Spring и Kafka, алгоритм регистрируется напрямую:

```java
DetectorRegistry registry = new SimpleDetectorRegistry(List.of(new MyLatencyDetector()));

DriftDetectorEngine engine = new DriftDetectorEngine(
        registry,
        new InMemoryDetectorStateStore(),
        List.of(new DetectorDefinition(
                "my-latency-detector",
                new MyLatencyConfig(50, 0.8),
                key -> key.metric().equals("latency")
        ))
);
```

Core не зависит от Spring, Kafka, REST API, UI и конкретного способа доставки метрик.

### Spring Boot подключение своего алгоритма

В Spring Boot приложении свой алгоритм можно подключить без форка DriftGuard. Достаточно объявить его как bean:

```java
@Configuration
class CustomDriftGuardConfiguration {
    @Bean
    DetectorAlgorithm<MyLatencyConfig, MyLatencyState> myLatencyDetector() {
        return new MyLatencyDetector();
    }
}
```

Starter автоматически добавит этот bean в общий `DetectorRegistry` вместе со встроенными алгоритмами.

Если алгоритму нужна собственная типизированная конфигурация, добавьте `DetectorDefinitionProvider`:

```java
@Bean
DetectorDefinitionProvider myDetectorDefinitions() {
    return () -> List.of(
            new DetectorDefinition(
                    "checkout-latency-my-detector",
                    new MyLatencyConfig(50, 0.8),
                    key -> key.service().equals("checkout-service")
                            && key.metric().equals("latency")
            )
    );
}
```

Такой способ нужен, когда конфигурация algorithm-specific и не должна описываться через универсальный YAML.

### Пример собственного алгоритма

```java
public record MyLatencyConfig(
        int warmupSamples,
        double maxAllowedValue
) implements DetectorConfig {
    @Override
    public String algorithm() {
        return "my-latency-threshold";
    }
}
```

```java
public record MyLatencyState(
        long count
) implements DetectorState {
    @Override
    public String algorithm() {
        return "my-latency-threshold";
    }
}
```

```java
public final class MyLatencyDetector implements DetectorAlgorithm<MyLatencyConfig, MyLatencyState> {
    @Override
    public String name() {
        return "my-latency-threshold";
    }

    @Override
    public Class<MyLatencyConfig> configType() {
        return MyLatencyConfig.class;
    }

    @Override
    public MyLatencyState initialState(MyLatencyConfig config) {
        return new MyLatencyState(0);
    }

    @Override
    public DetectionResult<MyLatencyState> detect(
            MetricPoint point,
            MyLatencyState state,
            MyLatencyConfig config,
            DetectionContext context
    ) {
        MyLatencyState next = new MyLatencyState(state.count() + 1);
        if (next.count() < config.warmupSamples() || point.value() <= config.maxAllowedValue()) {
            return DetectionResult.noDrift(next);
        }

        DriftEvent event = new DriftEvent(
                null,
                point.key(),
                point.timestamp(),
                point.timestamp(),
                point.timestamp(),
                DriftDirection.UP,
                DriftSeverity.CRITICAL,
                point.value(),
                point.value(),
                config.maxAllowedValue(),
                context.detectorName(),
                name(),
                "Latency exceeded custom threshold",
                point.tags(),
                Map.of("maxAllowedValue", config.maxAllowedValue())
        );

        return DetectionResult.drift(next, event);
    }
}
```

### Что лучше конфигурировать через YAML, а что через Java

Через YAML удобно описывать встроенные алгоритмы:

```text
page-hinkley
adwin
psi
ks
chi-square
```

Через Java `DetectorDefinitionProvider` лучше подключать:

```text
кастомные алгоритмы с собственным config-классом;
сложную selector-логику;
динамически собираемые detector definitions;
экспериментальные detector-ы, которые не должны попадать в общий YAML mapping.
```

## Архитектурные принципы

- Core не зависит от Spring, Kafka, Jackson и Micrometer.
- Алгоритмы зависят только от core.
- Kafka и Spring являются adapter-слоями.
- Demo не является основной системой, а только показывает использование библиотеки.
- Имена topic-ов, detector-ы, окна, пороги и emission policy задаются конфигурацией.
- Новый алгоритм добавляется через `DetectorAlgorithm<C, S>`.
- Кастомные detector definitions можно добавлять через `DetectorDefinitionProvider`.
- Внешняя доставка событий добавляется через `DriftEventSink`.
- Состояние detector-а хранится через `DetectorStateStore`.
- Kafka state и Spring lifecycle не попадают в core-логику.

## Используемые библиотеки

- Lombok применяется в `driftguard-spring-boot-starter` для configuration properties, где он убирает однотипные getters/setters.
- Apache Commons Math применяется в `driftguard-algorithms` для статистических тестов KS и chi-square.
- Jackson применяется в `driftguard-kafka` для JSON SerDes.
- Springdoc OpenAPI применяется в `driftguard-demo` для Swagger UI и `/v3/api-docs`.
- React, TypeScript, TanStack Query и Apache ECharts применяются в `driftguard-demo` для dashboard UI и графиков потоковых метрик.

## Demo-хранилище событий

`driftguard-demo` содержит in-memory repository для последних drift events:

```text
DemoDriftEventRepository
InMemoryDemoDriftEventRepository
DemoStoredDriftEvent
```

Это demo-specific слой, не часть core-библиотеки. Он нужен, чтобы UI мог показывать последние события независимо от источника:

```text
synthetic
live
kafka
```

Позже этот слой можно заменить JDBC/PostgreSQL adapter-ом без изменения core.

## Рекомендации по расширению проекта

Ближайшие полезные направления:

```text
driftguard-storage-jdbc
  JDBC/PostgreSQL adapter для хранения DriftEvent.

driftguard-micrometer
  Дополнительные Micrometer binders для detector latency, event count и quality metrics.

driftguard-grafana
  Готовый dashboard JSON для demo и production-like стенда.

driftguard-algorithms-extra
  Дополнительные алгоритмы: EWMA, seasonal baseline, robust z-score, MAD.
```

Эти расширения можно добавлять без изменения core-контрактов.

## Проверка проекта

Полная проверка:

```bash
mvn clean test
```

Проверка frontend:

```bash
cd driftguard-demo/src/main/frontend
npm ci
npm run build
```

Запуск demo локально:

```bash
mvn -pl driftguard-demo spring-boot:run
```

Запуск инфраструктуры:

```bash
docker compose up --build
```
