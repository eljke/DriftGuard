package ru.eljke.driftguard.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Kafka Serde backed by the DriftGuard JSON serializer and deserializer.
 */
public final class JsonSerde<T> implements Serde<T> {
    private final JsonSerializer<T> serializer;
    private final JsonDeserializer<T> deserializer;

    public JsonSerde(ObjectMapper objectMapper, Class<T> type) {
        this.serializer = new JsonSerializer<>(objectMapper);
        this.deserializer = new JsonDeserializer<>(objectMapper, type);
    }

    @Override
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<T> deserializer() {
        return deserializer;
    }
}


