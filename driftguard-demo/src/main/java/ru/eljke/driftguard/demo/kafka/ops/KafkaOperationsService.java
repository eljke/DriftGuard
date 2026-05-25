package ru.eljke.driftguard.demo.kafka.ops;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import ru.eljke.driftguard.demo.kafka.KafkaDemoService;
import ru.eljke.driftguard.demo.kafka.KafkaDemoStatus;
import ru.eljke.driftguard.spring.DriftGuardProperties;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * English demo documentation.
 */
@Service
public class KafkaOperationsService {
    static final String POINTS = "driftguard.kafka.detection.points";
    static final String EVENTS = "driftguard.kafka.detection.events";
    static final String ERRORS = "driftguard.kafka.detection.errors";
    static final String ROUTED_ERRORS = "driftguard.kafka.detection.errors.routed";
    static final String DURATION = "driftguard.kafka.detection.duration";

    private final KafkaDemoService kafkaDemoService;
    private final DriftGuardProperties driftGuardProperties;
    private final MeterRegistry meterRegistry;

    public KafkaOperationsService(
            KafkaDemoService kafkaDemoService,
            DriftGuardProperties driftGuardProperties,
            ObjectProvider<MeterRegistry> meterRegistry
    ) {
        this.kafkaDemoService = kafkaDemoService;
        this.driftGuardProperties = driftGuardProperties;
        this.meterRegistry = meterRegistry.getIfAvailable();
    }

    public KafkaOperationsSnapshot snapshot() {
        KafkaDemoStatus status = kafkaDemoService.status();
        DriftGuardProperties.KafkaProperties kafka = driftGuardProperties.getKafka();
        KafkaOperationsMetrics metrics = metrics();
        return new KafkaOperationsSnapshot(
                status.enabled(),
                status.running(),
                status.replay(),
                status.scenario(),
                status.inputTopic(),
                status.outputTopic(),
                status.bootstrapServers(),
                status.producedPoints(),
                status.totalPoints(),
                status.consumedEvents().size(),
                progress(status.producedPoints(), status.totalPoints()),
                kafka.getApplicationId(),
                List.copyOf(kafka.getInputTopics()),
                kafka.getOutputTopic(),
                readStringProperty(kafka, "getRuntimeStateStoreName", "driftguard-runtime-state"),
                readStringProperty(kafka, "getDetectionErrorMode", "FAIL_FAST"),
                meterRegistry != null,
                metrics,
                status.error()
        );
    }

    private KafkaOperationsMetrics metrics() {
        if (meterRegistry == null) {
            return KafkaOperationsMetrics.empty();
        }
        Timer timer = meterRegistry.find(DURATION).timer();
        long durationCount = timer == null ? 0L : timer.count();
        double totalMillis = timer == null ? 0.0 : timer.totalTime(TimeUnit.MILLISECONDS);
        double maxMillis = timer == null ? 0.0 : timer.max(TimeUnit.MILLISECONDS);
        double meanMillis = durationCount == 0 ? 0.0 : totalMillis / durationCount;
        return new KafkaOperationsMetrics(
                sumCounters(POINTS),
                sumCounters(EVENTS),
                sumCounters(ERRORS),
                sumCounters(ROUTED_ERRORS),
                durationCount,
                totalMillis,
                maxMillis,
                meanMillis
        );
    }

    private double sumCounters(String name) {
        if (meterRegistry == null) {
            return 0.0;
        }
        return meterRegistry.find(name)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private static String readStringProperty(Object source, String getterName, String fallback) {
        try {
            Object value = source.getClass().getMethod(getterName).invoke(source);
            return value == null ? fallback : value.toString();
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }

    private static double progress(int produced, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.clamp(produced * 100.0 / total, 0.0, 100.0);
    }
}


