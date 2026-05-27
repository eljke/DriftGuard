package ru.eljke.driftguard.spring;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.eljke.driftguard.core.alert.DefaultDriftAlertMapper;
import ru.eljke.driftguard.core.alert.DriftAlert;
import ru.eljke.driftguard.core.domain.DriftDirection;
import ru.eljke.driftguard.core.domain.DriftEvent;
import ru.eljke.driftguard.core.domain.DriftSeverity;
import ru.eljke.driftguard.core.domain.MetricKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDriftAlertSinkTest {
    @Test
    void postsAlertJsonToWebhook() throws IOException {
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/alerts", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        try {
            DriftGuardProperties.WebhookAlertProperties properties = new DriftGuardProperties.WebhookAlertProperties();
            properties.setUrl("http://localhost:" + server.getAddress().getPort() + "/alerts");
            WebhookDriftAlertSink sink = new WebhookDriftAlertSink(properties);

            sink.accept(alert());

            assertThat(body.get())
                    .contains("\"severity\":\"CRITICAL\"")
                    .contains("\"service\":\"checkout-service\"")
                    .contains("\"metric\":\"latency\"");
        } finally {
            server.stop(0);
        }
    }

    private static DriftAlert alert() {
        return new DefaultDriftAlertMapper().map(new DriftEvent(
                "event-1",
                MetricKey.builder()
                        .service("checkout-service")
                        .metric("latency")
                        .operation("POST /checkout")
                        .build(),
                Instant.parse("2026-05-01T10:00:00Z"),
                Instant.parse("2026-05-01T09:59:00Z"),
                Instant.parse("2026-05-01T10:00:00Z"),
                DriftDirection.UP,
                DriftSeverity.CRITICAL,
                3.0,
                300.0,
                100.0,
                "latency-page-hinkley",
                "page-hinkley",
                "mean shift detected",
                Map.of(),
                Map.of("relativeChangePercent", 200.0)
        ));
    }
}
