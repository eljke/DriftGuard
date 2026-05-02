package ru.eljke.driftguard.demo.kafka;

/**
 * Запрос на воспроизводимый replay synthetic-сценария через Kafka.
 *
 * @param scenario id synthetic-сценария
 * @param speed множитель скорости публикации точек
 * @param resetState нужно ли сбросить runtime detector state перед replay
 * @param profile detector profile, который нужно применить перед replay
 */
public record KafkaReplayRequest(
        String scenario,
        double speed,
        boolean resetState,
        String profile
) {
    public String normalizedScenario() {
        return scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
    }

    public double normalizedSpeed() {
        return speed <= 0.0 ? 1.0 : Math.min(speed, 20.0);
    }
}
