package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import ru.eljke.driftguard.core.error.DriftGuardException;

/**
 * Kafka JSON deserializer для конкретного DriftGuard-типа.
 */
public final class JsonDeserializer<T> implements Deserializer<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonDeserializer(ObjectMapper objectMapper, Class<T> type) {
        this.objectMapper = objectMapper;
        this.type = type;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(data, type);
        } catch (Exception e) {
            throw new DriftGuardException(KafkaDriftGuardErrorReason.JSON_DESERIALIZATION_FAILED, e, type.getName());
        }
    }
}
