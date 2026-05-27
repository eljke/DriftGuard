package ru.eljke.driftguard.spring.input;
import ru.eljke.driftguard.spring.alert.*;
import ru.eljke.driftguard.spring.autoconfigure.*;
import ru.eljke.driftguard.spring.input.*;
import ru.eljke.driftguard.spring.kafka.*;
import ru.eljke.driftguard.spring.metrics.*;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.domain.MetricKind;
import ru.eljke.driftguard.core.domain.MetricPoint;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerMetricInputPollerTest {
    @Test
    void samplesConfiguredGaugeAsMetricPoint() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AtomicReference<Double> latency = new AtomicReference<>(125.0);
        Gauge.builder("checkout.latency", latency, AtomicReference::get)
                .tag("operation", "POST /checkout")
                .register(meterRegistry);
        List<MetricPoint> published = new ArrayList<>();

        MicrometerMetricInputPoller poller = new MicrometerMetricInputPoller(
                meterRegistry,
                point -> {
                    published.add(point);
                    return List.of();
                },
                properties()
        );

        poller.pollOnce();

        assertThat(published).hasSize(1);
        MetricPoint point = published.getFirst();
        assertThat(point.key().service()).isEqualTo("checkout-service");
        assertThat(point.key().metric()).isEqualTo("latency");
        assertThat(point.key().operation()).isEqualTo("POST /checkout");
        assertThat(point.value()).isEqualTo(125.0);
        assertThat(point.kind()).isEqualTo(MetricKind.DURATION);
        assertThat(point.tags()).containsEntry("operation", "POST /checkout");
    }

    private static DriftGuardProperties.MicrometerInputProperties properties() {
        DriftGuardProperties.MicrometerMeterProperties meter = new DriftGuardProperties.MicrometerMeterProperties();
        meter.setName("checkout.latency");
        meter.setService("checkout-service");
        meter.setMetric("latency");
        meter.setOperation("POST /checkout");
        meter.setKind(MetricKind.DURATION);
        meter.setTags(Map.of("operation", "POST /checkout"));

        DriftGuardProperties.MicrometerInputProperties properties = new DriftGuardProperties.MicrometerInputProperties();
        properties.setEnabled(true);
        properties.setAutoStartup(false);
        properties.setPollInterval(Duration.ofSeconds(1));
        properties.setMeters(List.of(meter));
        return properties;
    }
}
