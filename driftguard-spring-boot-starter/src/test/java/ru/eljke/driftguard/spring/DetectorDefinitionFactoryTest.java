package ru.eljke.driftguard.spring;

import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.config.DetectorDefinition;

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
}
