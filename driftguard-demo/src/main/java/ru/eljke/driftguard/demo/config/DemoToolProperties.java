package ru.eljke.driftguard.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * English demo documentation.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "demo.tools")
public class DemoToolProperties {
    /**
     * English demo documentation.
     */
    private String kafkaUiUrl = "http://localhost:8090";

    /**
     * English demo documentation.
     */
    private String prometheusUrl = "http://localhost:9090";

    /**
     * English demo documentation.
     */
    private String grafanaUrl = "http://localhost:3000";

    /**
     * URL Swagger UI demo-application.
     */
    private String swaggerUrl = "/swagger-ui.html";
}


