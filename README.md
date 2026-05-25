# DriftGuard

DriftGuard is a modular Java library for detecting data drift in streaming technical metrics and publishing drift alerts. The project is structured as a reusable library plus integration modules; the demo module is intentionally kept separate so it can be moved into a standalone product that consumes the library.

## Modules

| Module | Purpose |
| --- | --- |
| `driftguard-core` | Domain model, detector contracts, `DriftGuard` facade, `DriftDetectorEngine`, state-store abstractions and event sinks. |
| `driftguard-algorithms` | Built-in drift detectors: Page-Hinkley, ADWIN, PSI, Kolmogorov-Smirnov and chi-square. |
| `driftguard-kafka` | Kafka JSON SerDes and Kafka Streams topology builders. |
| `driftguard-spring-boot-starter` | Spring Boot auto-configuration and `application.yml` binding. |
| `driftguard-testkit` | Reproducible synthetic metric scenarios, expected drift intervals and quality gates. |
| `driftguard-demo` | A demo observability product that uses DriftGuard to detect and alert on synthetic service degradation. |

## Runtime Flow

```text
Metric source
  -> MetricPoint
  -> DriftGuard / DriftDetectorEngine
  -> DetectorAlgorithm
  -> DetectorRuntimeStateStore
  -> DriftEvent
  -> DriftEventSink
```

`MetricPoint` is the stable input contract. `MetricKey` identifies the metric stream by `service`, `metric`, optional `instance` and optional `operation`.

`DetectorDefinition` binds a named detector instance to a typed algorithm configuration, a metric selector and an emission policy. Several definitions may use the same algorithm with different thresholds or selectors.

`DriftEvent` is the stable output contract for REST APIs, Kafka topics, UI tables, logs and custom alert sinks. Public events have lifecycle phases:

```text
STARTED    first public event for a drift episode
ONGOING    repeated update after emission cooldown
RECOVERED  recovery event after enough normal points near baseline
```

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

This mode is useful for unit tests, offline replay, embedded ingestion and custom alert pipelines.

## Built-In Algorithms

The algorithms are meant to complement each other rather than compete for the same signal:

| Algorithm | Best fit |
| --- | --- |
| `page-hinkley` | Fast online detection of sustained mean shifts. Good for latency, error rate, queue size, CPU and memory. Configure `DOWN` for throughput drops. |
| `adwin` | Adaptive-window online mean-shift detection. It uses a variance-aware ADWIN cut bound and shrinks the window after a confirmed change. |
| `psi` | Population Stability Index for broad distribution drift in binned values. Good for comparing current behavior with a baseline distribution. |
| `ks` | Two-sample Kolmogorov-Smirnov test for continuous distributions when raw samples are meaningful. |
| `chi-square` | Binned chi-square test for categorical or bucketed distributions with enough expected observations per bucket. |

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

## Demo Product Direction

`driftguard-demo` currently remains a Maven module, but it is treated as a standalone consumer product. It simulates a small service-observability environment:

- checkout latency degradation;
- error-rate spikes;
- throughput drops;
- queue backlog growth;
- seasonal latency without expected drift;
- multi-service Kafka replay.

The demo uses DriftGuard as a dependency, exposes REST/OpenAPI endpoints, renders a React UI, stores recent drift events and shows quality metrics. This makes it easy to move the module into a separate `DriftGuardDemo` repository later without changing the library contracts.

## Local Run

Run all tests:

```bash
mvn test
```

Run the full demo stack:

```bash
docker compose up --build
```

Services:

- Demo UI: `http://localhost:8080`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8090`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin` / `admin`)

Frontend development:

```bash
cd driftguard-demo/src/main/frontend
npm ci
npm run dev
```

Vite proxies `/api`, `/actuator`, `/v3` and Swagger requests to `localhost:8080`.

## Quality Gates

`driftguard-testkit` provides scenario generators and quality gates for precision, recall, false positives, missed drift intervals and first-detection delay. These tests protect algorithm behavior from regressions and make the project demonstrable for academic review.
