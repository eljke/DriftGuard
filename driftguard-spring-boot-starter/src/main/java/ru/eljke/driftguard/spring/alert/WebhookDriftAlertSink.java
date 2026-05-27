package ru.eljke.driftguard.spring.alert;

import ru.eljke.driftguard.core.alert.DriftAlert;
import ru.eljke.driftguard.core.alert.DriftAlertSink;
import ru.eljke.driftguard.spring.autoconfigure.DriftGuardProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generic webhook alert sink for chat bots, incident routers and automation
 * endpoints that accept JSON over HTTP.
 */
public final class WebhookDriftAlertSink implements DriftAlertSink {
    private final HttpClient client;
    private final URI endpoint;
    private final Duration timeout;
    private final Map<String, String> headers;

    public WebhookDriftAlertSink(DriftGuardProperties.WebhookAlertProperties properties) {
        this(
                HttpClient.newBuilder().connectTimeout(properties(properties).getTimeout()).build(),
                endpoint(properties(properties).getUrl()),
                properties(properties).getTimeout(),
                properties(properties).getHeaders()
        );
    }

    WebhookDriftAlertSink(HttpClient client, URI endpoint, Duration timeout, Map<String, String> headers) {
        this.client = client;
        this.endpoint = endpoint;
        this.timeout = timeout == null ? Duration.ofSeconds(3) : timeout;
        this.headers = Map.copyOf(headers == null ? Map.of() : headers);
    }

    @Override
    public void accept(DriftAlert alert) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json(alert)));
        headers.forEach(builder::header);
        try {
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Webhook alert sink returned HTTP " + response.statusCode());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Webhook alert sink failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Webhook alert sink interrupted", exception);
        }
    }

    private static String json(DriftAlert alert) {
        return """
                {"id":"%s","severity":"%s","title":"%s","message":"%s","service":"%s","metric":"%s","operation":"%s","labels":%s}
                """.formatted(
                escape(alert.id()),
                alert.severity(),
                escape(alert.title()),
                escape(alert.message()),
                escape(alert.key().service()),
                escape(alert.key().metric()),
                escape(alert.key().operation()),
                labels(alert.labels())
        ).trim();
    }

    private static String labels(Map<String, String> labels) {
        return (labels == null ? Map.<String, String>of() : labels).entrySet().stream()
                .map(entry -> "\"%s\":\"%s\"".formatted(escape(entry.getKey()), escape(entry.getValue())))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static URI endpoint(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("driftguard.alerts.webhook.url must be configured");
        }
        return URI.create(url);
    }

    private static DriftGuardProperties.WebhookAlertProperties properties(
            DriftGuardProperties.WebhookAlertProperties properties
    ) {
        return Objects.requireNonNull(properties, "properties must not be null");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
