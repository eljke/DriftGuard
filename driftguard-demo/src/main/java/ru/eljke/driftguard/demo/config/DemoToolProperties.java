package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Ссылки на вспомогательные инструменты локального demo-стенда.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.tools")
public class DemoToolProperties {
    /**
     * URL Kafka UI из docker compose.
     */
    private String kafkaUiUrl = "http://localhost:8090";

    /**
     * URL Prometheus из docker compose.
     */
    private String prometheusUrl = "http://localhost:9090";

    /**
     * URL Grafana из docker compose.
     */
    private String grafanaUrl = "http://localhost:3000";

    /**
     * URL Swagger UI demo-приложения.
     */
    private String swaggerUrl = "/swagger-ui.html";
}
