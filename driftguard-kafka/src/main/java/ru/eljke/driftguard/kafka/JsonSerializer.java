package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka JSON serializer для DriftGuard DTO и record-типов.
 */
public final class JsonSerializer<T> implements Serializer<T> {
    private final ObjectMapper objectMapper;

    public JsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize JSON for topic " + topic, e);
        }
    }
}
