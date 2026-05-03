package ru.eljke.driftguard.core.state;

import ru.eljke.driftguard.core.detector.DetectorState;
import ru.eljke.driftguard.core.error.DriftGuardErrors;

/**
 * Кодек для сериализации снимков состояния конкретного detector-а.
 *
 * <p>Core-модуль намеренно не выбирает JSON, Avro, Protobuf или другой физический формат.
 * Инфраструктурные адаптеры предоставляют конкретные кодеки и используют этот SPI,
 * чтобы сохранять opaque-реализации {@link DetectorState} без протаскивания зависимостей
 * адаптеров в доменную модель.</p>
 *
 * @param <S> конкретный тип состояния detector-а, который поддерживает этот кодек
 */
public interface DetectorStateCodec<S extends DetectorState> {
    /**
     * Стабильное имя алгоритма, возвращаемое {@link DetectorState#algorithm()}.
     */
    String algorithm();

    /**
     * Конкретный класс состояния, который обрабатывает этот кодек.
     */
    Class<S> stateType();

    /**
     * Сериализует конкретное состояние detector-а в байты формата адаптера.
     */
    byte[] serialize(S state);

    /**
     * Восстанавливает конкретное состояние detector-а из байтов формата адаптера.
     */
    S deserialize(byte[] payload);

    /**
     * Runtime-safe точка входа для сериализации из инфраструктурных адаптеров.
     */
    default byte[] serializeState(DetectorState state) {
        DriftGuardErrors.requireNonNull(state, "state");
        return serialize(stateType().cast(state));
    }
}
