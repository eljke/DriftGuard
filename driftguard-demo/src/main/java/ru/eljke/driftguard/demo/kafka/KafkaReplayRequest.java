package ru.eljke.driftguard.demo.kafka;

import ru.eljke.driftguard.demo.scenario.DemoScenarioRequest;

/**
 * English demo documentation.
 *
 * @param scenario documented value
 * @param speed documented value
 * @param resetState documented value
 * @param profile documented value
 * @param samples documented value
 * @param baselineValue stable value before drift
 * @param driftValue value during step/drop/spike drift
 * @param noiseStdDev noise standard deviation
 * @param driftStartPercent drift start position as a percentage of stream length
 * @param spikeLengthPercent spike duration as a percentage of stream length
 */
public record KafkaReplayRequest(
        String scenario,
        double speed,
        boolean resetState,
        String profile,
        Integer samples,
        Double baselineValue,
        Double driftValue,
        Double noiseStdDev,
        Double driftStartPercent,
        Double spikeLengthPercent
) {
    public String normalizedScenario() {
        return scenario == null || scenario.isBlank() ? "latency-step" : scenario.trim();
    }

    public double normalizedSpeed() {
        return speed <= 0.0 ? 1.0 : Math.min(speed, 20.0);
    }

    public int normalizedSamples(int fallback) {
        int value = samples == null || samples <= 0 ? fallback : samples;
        return Math.max(80, Math.min(2000, value));
    }

    public DemoScenarioRequest scenarioRequest() {
        return new DemoScenarioRequest(
                samples,
                baselineValue,
                driftValue,
                noiseStdDev,
                driftStartPercent,
                spikeLengthPercent
        );
    }
}


