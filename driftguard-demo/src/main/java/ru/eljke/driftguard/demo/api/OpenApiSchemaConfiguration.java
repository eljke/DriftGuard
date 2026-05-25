package ru.eljke.driftguard.demo.api;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * English demo documentation.
 */
@SuppressWarnings("rawtypes")
@Configuration
public class OpenApiSchemaConfiguration {
    @Bean
    public OpenApiCustomizer driftGuardSchemaDescriptions() {
        return openApi -> {
            Map<String, Schema> schemas = openApi.getComponents().getSchemas();
            describe(schemas, "DemoRunResult", "English demo text.", Map.of(
                    "scenario", "English demo text.",
                    "title", "English demo text.",
                    "metricPoints", "English demo text.",
                    "samplePoints", "English demo text.",
                    "expectedDrifts", "English demo text.",
                    "events", "English demo text.",
                    "quality", "English demo text."
            ));
            describe(schemas, "DetectionMetrics", "English demo text.", Map.of(
                    "events", "English demo text.",
                    "truePositiveEvents", "English demo text.",
                    "falsePositiveEvents", "English demo text.",
                    "expectedDriftIntervals", "English demo text.",
                    "detectedDriftIntervals", "English demo text.",
                    "missedDriftIntervals", "English demo text.",
                    "detected", "English demo text.",
                    "firstDetectionDelay", "English demo text.",
                    "precision", "English demo text.",
                    "recall", "English demo text."
            ));
            describe(schemas, "DriftEvent", "English demo text.", Map.ofEntries(
                    Map.entry("id", "Schema field."),
                    Map.entry("key", "Schema field."),
                    Map.entry("detectedAt", "Schema field."),
                    Map.entry("direction", "Schema field."),
                    Map.entry("severity", "Schema field."),
                    Map.entry("score", "Schema field."),
                    Map.entry("currentValue", "Schema field."),
                    Map.entry("baselineValue", "Schema field."),
                    Map.entry("detector", "Schema field."),
                    Map.entry("algorithm", "Schema field."),
                    Map.entry("reason", "Schema field.")
            ));
            describe(schemas, "MetricPoint", "English demo text.", Map.of(
                    "key", "English demo text.",
                    "timestamp", "English demo text.",
                    "value", "English demo text.",
                    "kind", "English demo text.",
                    "tags", "English demo text.",
                    "attributes", "English demo text."
            ));
            describe(schemas, "MetricKey", "English demo text.", Map.of(
                    "service", "English demo text.",
                    "metric", "English demo text.",
                    "instance", "English demo text.",
                    "operation", "English demo text."
            ));
            describe(schemas, "DriftInterval", "English demo text.", Map.of(
                    "start", "English demo text.",
                    "end", "English demo text."
            ));
            describe(schemas, "DemoScenarioDescriptor", "English demo text.", Map.of(
                    "id", "English demo text.",
                    "title", "English demo text.",
                    "metric", "English demo text.",
                    "description", "English demo text."
            ));
            describe(schemas, "KafkaDemoStatus", "English demo text.", Map.ofEntries(
                    Map.entry("enabled", "Schema field."),
                    Map.entry("running", "Schema field."),
                    Map.entry("scenario", "Schema field."),
                    Map.entry("inputTopic", "Schema field."),
                    Map.entry("outputTopic", "Schema field."),
                    Map.entry("bootstrapServers", "Kafka bootstrap servers."),
                    Map.entry("producedPoints", "Schema field."),
                    Map.entry("totalPoints", "Schema field."),
                    Map.entry("producers", "Schema field."),
                    Map.entry("consumedEvents", "Schema field."),
                    Map.entry("samplePoints", "Schema field."),
                    Map.entry("error", "Schema field.")
            ));
            describe(schemas, "KafkaProducerStatus", "English demo text.", Map.of(
                    "id", "English demo text.",
                    "service", "English demo text.",
                    "metric", "English demo text.",
                    "operation", "English demo text.",
                    "producedPoints", "English demo text.",
                    "totalPoints", "English demo text.",
                    "running", "English demo text."
            ));
            describe(schemas, "ToolLink", "English demo text.", Map.of(
                    "id", "English demo text.",
                    "title", "English demo text.",
                    "url", "English demo text.",
                    "description", "English demo text."
            ));
            describe(schemas, "DemoConfigurationView", "English demo text.", Map.of(
                    "aggressiveness", "English demo text.",
                    "kafka", "English demo text.",
                    "detectors", "English demo text."
            ));
            describe(schemas, "AggressivenessView", "English demo text.", Map.of(
                    "level", "English demo text.",
                    "description", "English demo text."
            ));
            describe(schemas, "KafkaConfigurationView", "English demo text.", Map.of(
                    "demoEnabled", "English demo text.",
                    "bootstrapServers", "Kafka bootstrap servers.",
                    "inputTopic", "English demo text.",
                    "outputTopic", "English demo text.",
                    "applicationId", "Kafka Streams application id.",
                    "playbackInterval", "English demo text."
            ));
            describe(schemas, "DemoScenarioRequest", "English demo text.", Map.of(
                    "samples", "English demo text.",
                    "baselineValue", "English demo text.",
                    "driftValue", "English demo text.",
                    "noiseStdDev", "English demo text.",
                    "driftStartPercent", "English demo text.",
                    "spikeLengthPercent", "English demo text."
            ));
            describe(schemas, "KafkaReplayRequest", "English demo text.", Map.of(
                    "scenario", "Id synthetic scenario.",
                    "speed", "English demo text.",
                    "resetState", "English demo text.",
                    "profile", "English demo text.",
                    "samples", "English demo text.",
                    "baselineValue", "English demo text.",
                    "driftValue", "English demo text.",
                    "noiseStdDev", "English demo text.",
                    "driftStartPercent", "English demo text.",
                    "spikeLengthPercent", "English demo text."
            ));
            describe(schemas, "DetectorConfigurationView", "English demo text.", Map.ofEntries(
                    Map.entry("name", "Schema field."),
                    Map.entry("algorithm", "Schema field."),
                    Map.entry("services", "Schema field."),
                    Map.entry("metrics", "Schema field."),
                    Map.entry("warningThreshold", "Schema field."),
                    Map.entry("criticalThreshold", "Schema field."),
                    Map.entry("warningPValue", "Schema field."),
                    Map.entry("criticalPValue", "Schema field."),
                    Map.entry("warmupSamples", "Schema field."),
                    Map.entry("emissionPolicy", "Schema field."),
                    Map.entry("sensitivity", "Schema field.")
            ));
            describe(schemas, "EmissionPolicyView", "English demo text.", Map.of(
                    "minConsecutiveSignals", "English demo text.",
                    "cooldown", "English demo text."
            ));
        };
    }

    private static void describe(Map<String, Schema> schemas, String name, String description, Map<String, String> properties) {
        Schema schema = schemas.get(name);
        if (schema == null) {
            return;
        }
        schema.setDescription(description);
        Map<String, Schema> schemaProperties = schema.getProperties();
        if (schemaProperties == null) {
            return;
        }
        properties.forEach((property, propertyDescription) -> {
            Schema propertySchema = schemaProperties.get(property);
            if (propertySchema != null) {
                propertySchema.setDescription(propertyDescription);
            }
        });
    }
}


