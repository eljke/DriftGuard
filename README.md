# DriftGuard

DriftGuard is a modular Java library for detecting data drift in streaming technical metrics and publishing drift alerts.
The repository contains reusable library modules and integration modules only.
The demo application lives in a separate sibling project: `DriftGuardDemo`.

## Modules

| Module                           | Purpose                                                                                                                 |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| `driftguard-core`                | Domain model, detector contracts, `DriftGuard` facade, `DriftDetectorEngine`, state-store abstractions and event sinks. |
| `driftguard-algorithms`          | Built-in drift detectors: Page-Hinkley, ADWIN, PSI, Kolmogorov-Smirnov and chi-square.                                  |
| `driftguard-kafka`               | Kafka JSON SerDes and Kafka Streams topology builders.                                                                  |
| `driftguard-spring-boot-starter` | Spring Boot auto-configuration and `application.yml` binding.                                                           |
| `driftguard-testkit`             | Reproducible synthetic metric scenarios, expected drift intervals and quality gates.                                    |

## Runtime Flow

```text
Metric source
  -> MetricPoint
  -> MetricPointPublisher / DriftGuard / DriftDetectorEngine
  -> DetectorAlgorithm
  -> DetectorRuntimeStateStore
  -> DriftEvent
  -> DriftEventSink / DriftAlertSink
```

`MetricPoint` is the stable input contract. `MetricKey` identifies the metric stream by `service`, `metric`, optional `instance` and optional `operation`.

`DetectorDefinition` binds a named detector instance to a typed algorithm configuration, a metric selector and an emission policy. Several definitions may use the same algorithm with different thresholds or selectors.

`DriftEvent` is the stable output contract for REST APIs, Kafka topics, UI tables, logs and custom alert sinks. Public events have lifecycle phases:

```text
STARTED    first public event for a drift episode
ONGOING    repeated update after emission cooldown
RECOVERED  recovery event after enough normal points near baseline
```

`DriftAlertSink` is the user-facing alert port. The core event stays structured for machines, while an alert adds a title, message and labels suitable for Telegram bots, Slack, email, logs or incident-management integrations.

## Embedded Usage

Use the builder API when DriftGuard is embedded directly in an application without Spring or Kafka:

```java
List<DriftEvent> publishedEvents = new ArrayList<>();

DriftGuard driftGuard = DriftGuard.builder()
        .registry(DefaultAlgorithms.registry())
        .definition(DetectorDefinition.builder()
                .name("latency-page-hinkley")
                .config(PageHinkleyConfig.builder()
                        .warmupSamples(20)
                        .delta(0.1)
                        .warningThreshold(25.0)
                        .criticalThreshold(50.0)
                        .alpha(0.05)
                        .build())
                .appliesTo(MetricSelector.builder()
                        .service("checkout-service")
                        .metric("latency")
                        .build())
                .emissionPolicy(EmissionPolicyConfig.builder()
                        .minConsecutiveSignals(2)
                        .cooldown(Duration.ofSeconds(30))
                        .build())
                .build())
        .sink(publishedEvents::add)
        .build();

MetricPoint point = MetricPoint.builder()
        .key(MetricKey.builder()
                .service("checkout-service")
                .metric("latency")
                .instance("instance-1")
                .operation("POST /checkout")
                .build())
        .timestamp(Instant.now())
        .value(250.0)
        .kind(MetricKind.DURATION)
        .build();

List<DriftEvent> events = driftGuard.detect(point);
```

The facade also implements `MetricPointPublisher`, so application code can depend on the narrow publishing port:

```java
MetricPointPublisher publisher = driftGuard;
publisher.publish(point);
```

## Built-In Algorithms

The algorithms are meant to complement each other rather than compete for the same signal:

| Algorithm      | Best fit                                                                                                                                         |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `page-hinkley` | Fast online detection of sustained mean shifts. Good for latency, error rate, queue size, CPU and memory. Configure `DOWN` for throughput drops. |
| `adwin`        | Adaptive-window online mean-shift detection. It uses a variance-aware ADWIN cut bound and shrinks the window after a confirmed change.           |
| `psi`          | Population Stability Index for broad distribution drift in binned values. Good for comparing current behavior with a baseline distribution.      |
| `ks`           | Two-sample Kolmogorov-Smirnov test for continuous distributions when raw samples are meaningful.                                                 |
| `chi-square`   | Binned chi-square test for categorical or bucketed distributions with enough expected observations per bucket.                                   |

Recommended combinations:

- Use Page-Hinkley for quick operational alerts on directional metric changes.
- Use ADWIN when the baseline should adapt online after the change is confirmed.
- Use PSI, KS or chi-square for slower distribution-level validation and diagnostics.
- Use emission policies to avoid alert spam when several detectors see the same incident.

## Spring Boot Usage

Add `driftguard-spring-boot-starter` and configure detectors in `application.yml`:

```yaml
driftguard:
  enabled: true
  detectors:
    - name: latency-page-hinkley
      algorithm: page-hinkley
      services: [checkout-service]
      metrics: [latency]
      warmup-samples: 20
      delta: 0.1
      warning-threshold: 25.0
      critical-threshold: 50.0
      alpha: 0.05
      emission-policy:
        min-consecutive-signals: 2
        cooldown: 30s
        recovery-consecutive-normal: 3
```

The starter creates a detector registry, runtime state store, detector definitions, `DriftDetectorEngine`, Micrometer listeners when a `MeterRegistry` is present, and a `DriftEventSinkListener` when custom `DriftEventSink` beans exist.

The starter also creates a `MetricPointPublisher` bean. A Spring application can publish observations without knowing the engine wiring:

```java
@Service
class CheckoutMetricsAdapter {
    private final MetricPointPublisher driftGuard;

    CheckoutMetricsAdapter(MetricPointPublisher driftGuard) {
        this.driftGuard = driftGuard;
    }

    void recordLatency(String operation, double millis) {
        driftGuard.publish(MetricPoint.builder()
                .key(MetricKey.builder()
                        .service("checkout-service")
                        .metric("latency")
                        .operation(operation)
                        .build())
                .timestamp(Instant.now())
                .value(millis)
                .kind(MetricKind.DURATION)
                .build());
    }
}
```

If an application already exposes useful Micrometer meters, the starter can poll selected meters and publish them automatically:

```yaml
driftguard:
  micrometer-input:
    enabled: true
    poll-interval: 5s
    meters:
      - name: http.server.requests
        type: timer-mean
        service: checkout-service
        metric: latency
        operation: POST /checkout
        tags:
          uri: /checkout
          method: POST
```

This adapter is intentionally explicit: DriftGuard still needs to know which meters are meaningful drift signals and how they map to `service`, `metric` and `operation`.

## Integration Quickstarts

### Spring Boot Embedded Service

Use this when the application owns the metric values directly:

```text
business operation -> MetricPointPublisher -> DriftGuard -> DriftAlertSink
```

Define detectors in `application.yml`, inject `MetricPointPublisher`, and publish one `MetricPoint` per observation. This keeps business code independent from the engine and registry classes.

### Micrometer Input Adapter

Use this when the application already exports useful Micrometer meters. Configure `driftguard.micrometer-input.meters` and let the starter poll selected meters. Prefer this for HTTP timers, queue gauges, JVM/runtime gauges and other already-instrumented signals.

### Kafka Streams Adapter

Use this when metric producers and drift detection are separate runtime components. Publish JSON `MetricPoint` messages to Kafka and let the DriftGuard topology write JSON `DriftEvent` messages to the output topic.

### Custom Alert Sink

Use this when alerts must leave the process through a product-specific channel. Define a `DriftAlertSink` bean. The default SLF4J sink remains available unless `driftguard.alerts.logging-enabled=false`.

```java
@Component
class WebhookDriftAlertSink implements DriftAlertSink {
    @Override
    public void accept(DriftAlert alert) {
        // Send alert.title(), alert.message() and alert.labels() to the target system.
    }
}
```

## Alerts

By default, the Spring Boot starter creates an SLF4J alert sink. Production applications normally replace it with a custom `DriftAlertSink` bean:

```java
@Component
class TelegramDriftAlertSink implements DriftAlertSink {
    private final TelegramClient telegram;

    TelegramDriftAlertSink(TelegramClient telegram) {
        this.telegram = telegram;
    }

    @Override
    public void accept(DriftAlert alert) {
        telegram.sendMessage(alert.message());
    }
}
```

Disable alert mapping or the default log sink when needed:

```yaml
driftguard:
  alerts:
    enabled: true
    logging-enabled: false
```

## Explainability

Every `DriftEvent` contains shared fields and algorithm-specific `details`.
User interfaces and alert sinks should display:

- lifecycle phase: `STARTED`, `ONGOING` or `RECOVERED`;
- metric identity: service, metric, operation and instance;
- current value, baseline value and score;
- threshold or p-value when available;
- algorithm details such as observation count, ADWIN cut, ADWIN bound, mean difference or Page-Hinkley cumulative score.

This makes alerts auditable: a reviewer can see both the business impact and the statistical evidence that triggered the event.

## Quality Gates

Use `driftguard-testkit` to validate changes against reproducible drift scenarios. The testkit reports precision, recall, false positives, missed drift intervals and first-detection delay. A practical release gate is:

- no missed drift on core scenarios that the selected profile claims to cover;
- false positives stay within the configured tolerance for stable and seasonal scenarios;
- first detection delay is documented for aggressive, balanced and conservative profiles;
- custom detectors include at least one stable scenario and one drift scenario.

These gates are especially important when detector thresholds, emission policies or alert recovery rules change.

## Project Roadmap

- Integrations: production-ready examples for Spring Boot embedded, Micrometer polling, Kafka Streams and webhook/chat alert delivery.
- Explainability: richer alert templates and UI evidence panels for each algorithm.
- Demo product: keep `Checkout Service` and `Kafka Service` as the default screens; keep synthetic screens as lab mode.
- Quality: benchmark reports per profile and per algorithm, generated from `driftguard-testkit`.
- Documentation: small quickstarts for each integration path and extension point.

## Custom Detectors

Custom algorithms are first-class extension points. Implement `DetectorAlgorithm<C, S>`, where `C` is a `DetectorConfig` and `S` is a `DetectorState`, then register a detector definition that uses the custom config.

With Spring Boot, expose the algorithm and definitions as beans:

```java
@Bean
DetectorAlgorithm<MyConfig, MyState> myDetectorAlgorithm() {
    return new MyDetectorAlgorithm();
}

@Bean
DetectorDefinitionProvider myDetectorDefinitions() {
    return () -> List.of(DetectorDefinition.builder()
            .name("business-score-detector")
            .config(new MyConfig())
            .appliesTo(MetricSelector.builder()
                    .service("scoring-service")
                    .metric("business-score")
                    .build())
            .build());
}
```

The starter adds custom algorithms to the registry alongside built-in algorithms.

## Kafka Streams Usage

The Kafka integration reads JSON `MetricPoint` messages and writes JSON `DriftEvent` messages:

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

Kafka message keys should identify the metric stream when possible, for example `checkout-service|latency|POST /checkout`. Stable keys improve partitioning and debugging.

## Standalone Demo

`DriftGuardDemo` is a standalone Spring Boot product that consumes this library. It simulates a small service-observability environment, runs synthetic and Kafka-backed drift scenarios, stores recent drift events, exposes REST/OpenAPI endpoints and renders a React UI.

Build the library artifacts first:

```bash
mvn install
```

Then run the demo from the sibling project:

```bash
cd ../DriftGuardDemo
mvn spring-boot:run
```

The demo UI is available at `http://localhost:8080`.

## Local Verification

Run all library tests:

```bash
mvn test
```

`driftguard-testkit` provides scenario generators and quality gates for precision, recall, false positives, missed drift intervals and first-detection delay. These tests protect algorithm behavior from regressions and make the project demonstrable for academic review.
