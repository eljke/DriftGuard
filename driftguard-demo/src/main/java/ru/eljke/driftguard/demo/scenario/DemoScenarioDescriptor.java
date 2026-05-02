package ru.eljke.driftguard.demo.scenario;

/**
 * Краткое описание demo-сценария для UI и REST API.
 */
public record DemoScenarioDescriptor(
        String id,
        String title,
        String metric,
        String description
) {
}
