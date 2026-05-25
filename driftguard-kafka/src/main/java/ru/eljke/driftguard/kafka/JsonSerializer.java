package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serializer;
import ru.eljke.driftguard.core.error.DriftGuardException;

/**
 * Kafka JSON serializer for DriftGuard DTOs and record types.
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
            throw new DriftGuardException(KafkaDriftGuardErrorReason.JSON_SERIALIZATION_FAILED, e, data.getClass().getName());
        }
    }
}


