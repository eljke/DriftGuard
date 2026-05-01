package ru.eljke.driftguard.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Настройки DriftGuard, читаемые из {@code application.yml}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "driftguard")
public class DriftGuardProperties {
    private boolean enabled = true;
    private List<DetectorProperties> detectors = new ArrayList<>();

    public void setDetectors(List<DetectorProperties> detectors) {
        this.detectors = detectors == null ? new ArrayList<>() : detectors;
    }

    @Getter
    @Setter
    public static class DetectorProperties {
        private String name;
        private String algorithm;
        private List<String> services = new ArrayList<>();
        private List<String> metrics = new ArrayList<>();
        private int baselineWindowSize = 40;
        private int currentWindowSize = 40;
        private int windowSize = 40;
        private int minSubWindowSize = 10;
        private int warmupSamples = 20;
        private int buckets = 5;
        private double warningThreshold = 0.1;
        private double criticalThreshold = 0.25;
        private double warningPValue = 0.05;
        private double criticalPValue = 0.01;
        private double delta = 0.1;
        private double alpha = 0.05;
        private double epsilon = 1e-4;
        private double criticalMultiplier = 2.0;
        private double minExpectedCount = 1.0;
        private EmissionPolicyProperties emissionPolicy = new EmissionPolicyProperties();

        public void setServices(List<String> services) {
            this.services = services == null ? new ArrayList<>() : services;
        }

        public void setMetrics(List<String> metrics) {
            this.metrics = metrics == null ? new ArrayList<>() : metrics;
        }

        public void setEmissionPolicy(EmissionPolicyProperties emissionPolicy) {
            this.emissionPolicy = emissionPolicy == null ? new EmissionPolicyProperties() : emissionPolicy;
        }
    }

    @Getter
    @Setter
    public static class EmissionPolicyProperties {
        private int minConsecutiveSignals = 1;
        private Duration cooldown = Duration.ZERO;
    }
}
