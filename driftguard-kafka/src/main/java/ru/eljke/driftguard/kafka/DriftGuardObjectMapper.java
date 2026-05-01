package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Factory Jackson mapper-а для Kafka JSON payload-ов DriftGuard.
 */
public final class DriftGuardObjectMapper {
    private DriftGuardObjectMapper() {
    }

    public static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .findAndRegisterModules();
    }
}
