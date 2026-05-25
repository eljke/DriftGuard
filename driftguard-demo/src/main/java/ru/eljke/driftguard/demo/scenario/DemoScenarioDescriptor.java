package ru.eljke.driftguard.demo.scenario;

/**
 * Short demo scenario description for UI and REST API.
 */
public record DemoScenarioDescriptor(
        String id,
        String title,
        String metric,
        String description
) {
}


