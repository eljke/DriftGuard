package ru.eljke.driftguard.algorithms.support;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.DoubleSummaryStatistics;

/**
 * Неизменяемое окно фиксированного размера для реализаций detector-ов.
 *
 * <p>Добавление значения возвращает новый экземпляр окна. Это упрощает анализ
 * snapshot-ов состояния алгоритма и исключает скрытую мутацию внутри detector-ов.</p>
 */
public final class SlidingDoubleWindow {
    private final int capacity;
    private final ArrayDeque<Double> values;

    public SlidingDoubleWindow(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
        this.values = new ArrayDeque<>(capacity);
    }

    private SlidingDoubleWindow(int capacity, ArrayDeque<Double> values) {
        this.capacity = capacity;
        this.values = values;
    }

    public SlidingDoubleWindow add(double value) {
        ArrayDeque<Double> next = new ArrayDeque<>(values);
        if (next.size() == capacity) {
            next.removeFirst();
        }
        next.addLast(value);
        return new SlidingDoubleWindow(capacity, next);
    }

    public int size() {
        return values.size();
    }

    public boolean isFull() {
        return values.size() == capacity;
    }

    public double[] toArray() {
        double[] result = new double[values.size()];
        int i = 0;
        for (double value : values) {
            result[i++] = value;
        }
        return result;
    }

    public double mean() {
        return Arrays.stream(toArray()).average().orElse(0.0);
    }

    public double variance() {
        double[] data = toArray();
        if (data.length < 2) {
            return 0.0;
        }
        double mean = Arrays.stream(data).average().orElse(0.0);
        double sum = 0.0;
        for (double value : data) {
            double diff = value - mean;
            sum += diff * diff;
        }
        return sum / (data.length - 1);
    }

    public DoubleSummaryStatistics summary() {
        return Arrays.stream(toArray()).summaryStatistics();
    }
}
