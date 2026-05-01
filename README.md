# DriftGuard

DriftGuard - модульная Java-платформа для обнаружения дрейфа потоковых технических метрик в распределённых системах.

Проект строится как набор библиотек и интеграционных модулей.

## Модули

- `driftguard-core` - доменная модель, контракты детекторов, engine обработки и абстракции хранения состояния.
- `driftguard-algorithms` - реализации алгоритмов обнаружения дрейфа: PSI, ADWIN-style, Page-Hinkley, KS и хи-квадрат.
- `driftguard-kafka` - слой интеграции с Kafka Streams.
- `driftguard-spring-boot-starter` - Spring Boot автоконфигурация.
- `driftguard-testkit` - генераторы синтетических потоков и сценариев деградации.
- `driftguard-demo` - демонстрационное Spring Boot приложение.

## Основная идея

Поток метрик поступает из любого источника: Kafka, REST, тестовый генератор или встроенный adapter. Интеграционный слой преобразует данные в `MetricPoint` и передаёт их в `DriftDetectorEngine`.

Engine выбирает подходящие detector definitions, получает состояние detector-а по ключу метрики, вызывает нужный алгоритм и возвращает `DriftEvent`, если drift подтверждён.

```text
metric source
  -> MetricPoint
  -> DriftDetectorEngine
  -> DetectorAlgorithm
  -> DetectorStateStore
  -> DriftEvent
```

## Правила Архитектуры

- `driftguard-core` не содержит зависимостей от инфраструктуры.
- Алгоритмы подключаются через контракт `DetectorAlgorithm`.
- Конфигурация алгоритмов типизирована через реализации `DetectorConfig`.
- Состояние хранится через интерфейс `DetectorStateStore`.
- Kafka, Spring и demo должны быть adapter-слоями поверх core, а не частью core-логики.

## Реализованные Алгоритмы

- `psi` - Population Stability Index для сравнения распределений baseline/current.
- `adwin` - MVP ADWIN-style detector на sliding window и Hoeffding bound.
- `page-hinkley` - онлайн-детектор сдвига среднего значения.
- `ks` - двухвыборочный критерий Колмогорова-Смирнова.
- `chi-square` - binned хи-квадрат тест распределений.

## Проверка

```bash
mvn test
```

Ожидаемый результат: `BUILD SUCCESS`.
