export type Severity = "INFO" | "WARNING" | "CRITICAL";

export interface MetricKey {
  service: string;
  metric: string;
  instance?: string;
  operation?: string;
}

export interface MetricPoint {
  key: MetricKey;
  timestamp: string;
  value: number;
  kind: string;
  tags: Record<string, string>;
  attributes: Record<string, unknown>;
}

export interface DriftEvent {
    id: string;
    key: MetricKey;
    detectedAt: string;
    phase: string;
    direction: string;
    detector: string;
    algorithm: string;
    severity: Severity;
    score: number;
    currentValue: number;
    baselineValue: number;
    baselineSummary?: string;
    currentSummary?: string;
    reason: string;
    metadata: Record<string, string>;
    tags: Record<string, string>;
    details: Record<string, unknown>;
}

export interface DetectionMetrics {
  detected: boolean;
  truePositive: boolean;
  falsePositive: boolean;
  missed: boolean;
  detectionDelaySamples: number;
  detectionDelay: string;
  events: number;
}

export interface DetectionBenchmarkReport {
  label: string;
  results: DetectionBenchmarkResult[];
  summary: DetectionBenchmarkSummary;
}

export interface DetectionBenchmarkResult {
  scenario: string;
  metrics: DetectionMetrics;
}

export interface DetectionBenchmarkSummary {
  scenarios: number;
  detectedScenarios: number;
  events: number;
  truePositiveEvents: number;
  falsePositiveEvents: number;
  expectedDriftIntervals: number;
  detectedDriftIntervals: number;
  missedDriftIntervals: number;
  precision: number;
  recall: number;
  meanFirstDetectionDelay: string;
}

export interface DriftInterval {
  start: string;
  end: string;
  description: string;
}

export interface DemoRunResult {
  scenario: string;
  title: string;
  mode: string;
  running: boolean;
  processedPoints: number;
  metricPoints: number;
  samplePoints: MetricPoint[];
  expectedDrifts: DriftInterval[];
  events: DriftEvent[];
  quality: DetectionMetrics;
}

export interface DemoScenarioDescriptor {
  id: string;
  title: string;
  metric: string;
  description: string;
}

export interface KafkaProducerStatus {
  id: string;
  service: string;
  metric: string;
  operation?: string;
  producedPoints: number;
  totalPoints: number;
  running: boolean;
}

export interface KafkaDemoStatus {
  enabled: boolean;
  running: boolean;
  scenario: string;
  inputTopic: string;
  outputTopic: string;
  bootstrapServers: string;
  producedPoints: number;
  totalPoints: number;
  producers: KafkaProducerStatus[];
  consumedEvents: DriftEvent[];
  samplePoints: MetricPoint[];
  error?: string;
}

export interface ToolLink {
  id: string;
  title: string;
  url: string;
  description: string;
}

export interface DemoConfigurationView {
  aggressiveness: {
    level: string;
    description: string;
  };
  availableProfiles: string[];
  registeredAlgorithms: string[];
  kafka: {
    demoEnabled: boolean;
    bootstrapServers: string;
    inputTopic: string;
    outputTopic: string;
    applicationId: string;
    playbackInterval: string;
  };
  detectors: DetectorConfigurationView[];
}

export interface DetectorConfigurationView {
  name: string;
  algorithm: string;
  services: string[];
  metrics: string[];
  warningThreshold: number;
  criticalThreshold: number;
  warningPValue: number;
  criticalPValue: number;
  warmupSamples: number;
  emissionPolicy: {
    minConsecutiveSignals: number;
    cooldown: string;
    recoveryConsecutiveNormal: number;
  };
  sensitivity: string;
}
