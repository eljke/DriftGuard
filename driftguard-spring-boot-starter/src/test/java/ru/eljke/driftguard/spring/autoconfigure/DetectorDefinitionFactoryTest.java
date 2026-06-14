package ru.eljke.driftguard.spring.autoconfigure;
import ru.eljke.driftguard.spring.alert.*;
import ru.eljke.driftguard.spring.autoconfigure.*;
import ru.eljke.driftguard.spring.input.*;
import ru.eljke.driftguard.spring.kafka.*;
import ru.eljke.driftguard.spring.metrics.*;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorDefinition;
import ru.eljke.driftguard.algorithms.adaptive.AdaptivePageHinkleyConfig;

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

    @Test
    void createsAdaptivePageHinkleyDefinition() {
        DriftGuardProperties.DetectorProperties detector = new DriftGuardProperties.DetectorProperties();
        detector.setName("adaptive-latency");
        detector.setAlgorithm("page-hinkley");
        detector.setProfile(DriftGuardProperties.DetectorProfile.ADAPTIVE);
        detector.setAdaptiveCalibrationSamples(80);
        detector.setWarningThreshold(25.0);
        detector.setCriticalThreshold(50.0);

        DriftGuardProperties properties = new DriftGuardProperties();
        properties.setDetectors(List.of(detector));

        AdaptivePageHinkleyConfig config = (AdaptivePageHinkleyConfig) DetectorDefinitionFactory.create(properties)
                .getFirst()
                .config();

        assertEquals(80, config.calibrationSamples());
        assertEquals(16.25, config.aggressive().warningThreshold());
        assertEquals(45.0, config.conservative().warningThreshold());
    }
}
