export type Severity = "INFO" | "WARNING" | "CRITICAL";

export interface MetricKey {
  service: string;
  metric: string;
  instance: string;
  operation?: string;
  tags: Record<string, string>;
}

export interface MetricPoint {
  key: MetricKey;
  timestamp: string;
  value: number;
  unit?: string;
  kind?: string;
  metadata: Record<string, string>;
}

export interface DriftEvent {
  id: string;
  key: MetricKey;
  detectedAt: string;
  detector: string;
  algorithm: string;
  severity: Severity;
  score: number;
  baselineSummary?: string;
  currentSummary?: string;
  reason: string;
  metadata: Record<string, string>;
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
  completed: boolean;
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
  };
  sensitivity: string;
}
