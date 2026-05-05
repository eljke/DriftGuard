package ru.eljke.driftguard.demo.kafka;

/**
 * Запрос на воспроизводимый replay synthetic-сценария через Kafka.
 *
 * @param scenario id synthetic-сценария
 * @param speed множитель скорости публикации точек
 * @param resetState нужно ли сбросить runtime detector state перед replay
 * @param profile detector profile, который нужно применить перед replay
 * @param samples сколько MetricPoint опубликовать на один producer stream
 */
public record KafkaReplayRequest(
        String scenario,
        double speed,
        boolean resetState,
        String profile,
        Integer samples
) {
    public String normalizedScenario() {
        return scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
    }

    public double normalizedSpeed() {
        return speed <= 0.0 ? 1.0 : Math.min(speed, 20.0);
    }

    public int normalizedSamples(int fallback) {
        int value = samples == null || samples <= 0 ? fallback : samples;
        return Math.max(80, Math.min(600, value));
    }
}
