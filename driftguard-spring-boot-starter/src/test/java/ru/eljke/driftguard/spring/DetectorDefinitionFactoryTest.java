package ru.eljke.driftguard.spring;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorDefinition;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DetectorDefinitionFactoryTest {
    @Test
    void createsDefaultDefinitionsWhenNoPropertiesProvided() {
        List<DetectorDefinition> definitions = DetectorDefinitionFactory.create(new DriftGuardProperties());

        assertFalse(definitions.isEmpty());
        assertEquals("latency-page-hinkley", definitions.getFirst().name());
    }

    @Test
    void mapsEmissionPolicyProperties() {
        DriftGuardProperties.DetectorProperties detector = new DriftGuardProperties.DetectorProperties();
        detector.setName("latency-stable-page-hinkley");
        detector.setAlgorithm("page-hinkley");
        detector.setMetrics(List.of("latency"));
        detector.getEmissionPolicy().setMinConsecutiveSignals(2);
        detector.getEmissionPolicy().setCooldown(Duration.ofSeconds(30));

        DriftGuardProperties properties = new DriftGuardProperties();
        properties.setDetectors(List.of(detector));

        DetectorDefinition definition = DetectorDefinitionFactory.create(properties).getFirst();

        assertEquals(2, definition.emissionPolicy().minConsecutiveSignals());
        assertEquals(Duration.ofSeconds(30), definition.emissionPolicy().cooldown());
    }
}
