package ru.eljke.driftguard.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Настройки DriftGuard, читаемые из {@code application.yml}.
 */
@ConfigurationProperties(prefix = "driftguard")
public class DriftGuardProperties {
    private boolean enabled = true;
    private List<DetectorProperties> detectors = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<DetectorProperties> getDetectors() {
        return detectors;
    }

    public void setDetectors(List<DetectorProperties> detectors) {
        this.detectors = detectors == null ? new ArrayList<>() : detectors;
    }

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public List<String> getServices() {
            return services;
        }

        public void setServices(List<String> services) {
            this.services = services == null ? new ArrayList<>() : services;
        }

        public List<String> getMetrics() {
            return metrics;
        }

        public void setMetrics(List<String> metrics) {
            this.metrics = metrics == null ? new ArrayList<>() : metrics;
        }

        public int getBaselineWindowSize() {
            return baselineWindowSize;
        }

        public void setBaselineWindowSize(int baselineWindowSize) {
            this.baselineWindowSize = baselineWindowSize;
        }

        public int getCurrentWindowSize() {
            return currentWindowSize;
        }

        public void setCurrentWindowSize(int currentWindowSize) {
            this.currentWindowSize = currentWindowSize;
        }

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }

        public int getMinSubWindowSize() {
            return minSubWindowSize;
        }

        public void setMinSubWindowSize(int minSubWindowSize) {
            this.minSubWindowSize = minSubWindowSize;
        }

        public int getWarmupSamples() {
            return warmupSamples;
        }

        public void setWarmupSamples(int warmupSamples) {
            this.warmupSamples = warmupSamples;
        }

        public int getBuckets() {
            return buckets;
        }

        public void setBuckets(int buckets) {
            this.buckets = buckets;
        }

        public double getWarningThreshold() {
            return warningThreshold;
        }

        public void setWarningThreshold(double warningThreshold) {
            this.warningThreshold = warningThreshold;
        }

        public double getCriticalThreshold() {
            return criticalThreshold;
        }

        public void setCriticalThreshold(double criticalThreshold) {
            this.criticalThreshold = criticalThreshold;
        }

        public double getWarningPValue() {
            return warningPValue;
        }

        public void setWarningPValue(double warningPValue) {
            this.warningPValue = warningPValue;
        }

        public double getCriticalPValue() {
            return criticalPValue;
        }

        public void setCriticalPValue(double criticalPValue) {
            this.criticalPValue = criticalPValue;
        }

        public double getDelta() {
            return delta;
        }

        public void setDelta(double delta) {
            this.delta = delta;
        }

        public double getAlpha() {
            return alpha;
        }

        public void setAlpha(double alpha) {
            this.alpha = alpha;
        }

        public double getEpsilon() {
            return epsilon;
        }

        public void setEpsilon(double epsilon) {
            this.epsilon = epsilon;
        }

        public double getCriticalMultiplier() {
            return criticalMultiplier;
        }

        public void setCriticalMultiplier(double criticalMultiplier) {
            this.criticalMultiplier = criticalMultiplier;
        }

        public double getMinExpectedCount() {
            return minExpectedCount;
        }

        public void setMinExpectedCount(double minExpectedCount) {
            this.minExpectedCount = minExpectedCount;
        }
    }
}
