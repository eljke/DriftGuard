import type {
  DemoCapabilityGroup,
  DemoConfigurationView,
  DetectionBenchmarkReport,
  DetectionMetrics,
  DemoHelp,
  DemoRunResult,
  DemoScenarioRequest,
  DemoScenarioDescriptor,
  DemoStoredDriftEvent,
  DriftEvent,
  KafkaDemoStatus,
  KafkaOperationsSnapshot,
  KafkaReplayRequest,
  ToolLink
} from "../types";

interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...init?.headers
    },
    ...init
  });
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  return response.json() as Promise<T>;
}

async function errorMessage(response: Response) {
  const body = await response.text();
  if (!body) {
    return `Request failed: ${response.status}`;
  }
  try {
    const error = JSON.parse(body) as Partial<ApiErrorResponse>;
    return error.message ? `${error.code ?? response.status}: ${error.message}` : body;
  } catch {
    return body;
  }
}

export const api = {
    overview: () => request<DemoRunResult>("/api/demo"),
    events: () => request<DriftEvent[]>("/api/demo/events"),
    storedEvents: () => request<DemoStoredDriftEvent[]>("/api/demo/events/stored"),
    clearStoredEvents: () => request<{ cleared: boolean }>("/api/demo/events/clear", { method: "POST" }),
    quality: () => request<DetectionMetrics>("/api/demo/quality"),
    scenarios: () => request<DemoScenarioDescriptor[]>("/api/demo/scenarios"),
    runScenario: (scenario: string, body?: DemoScenarioRequest) => request<DemoRunResult>(`/api/demo/run/${scenario}`, {
        method: "POST",
        body: JSON.stringify(body ?? {})
    }),
    startLive: (scenario: string, body?: DemoScenarioRequest) => request<DemoRunResult>(`/api/demo/live/${scenario}`, {
        method: "POST",
        body: JSON.stringify(body ?? {})
    }),
    stopLive: () => request<DemoRunResult>("/api/demo/live/stop", {method: "POST"}),
    benchmark: () => request<DetectionBenchmarkReport>("/api/demo/benchmark"),
    benchmarkProfiles: () => request<DetectionBenchmarkReport[]>("/api/demo/benchmark/profiles"),
    kafkaStatus: () => request<KafkaDemoStatus>("/api/demo/kafka"),
    kafkaOperations: () => request<KafkaOperationsSnapshot>("/api/demo/kafka/operations"),
    startKafka: (scenario: string, body?: DemoScenarioRequest) => request<KafkaDemoStatus>(`/api/demo/kafka/start/${scenario}`, {
        method: "POST",
        body: JSON.stringify(body ?? {})
    }),
    replayKafka: (body: KafkaReplayRequest) => request<KafkaDemoStatus>("/api/demo/kafka/replay", {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(body)
    }),
    stopKafka: () => request<KafkaDemoStatus>("/api/demo/kafka/stop", {method: "POST"}),
    tools: () => request<ToolLink[]>("/api/demo/tools"),
    help: () => request<DemoHelp>("/api/demo/help"),
    capabilities: () => request<DemoCapabilityGroup[]>("/api/demo/capabilities"),
    configuration: () => request<DemoConfigurationView>("/api/demo/configuration"),
    updateProfile: (profile: string) => request<DemoConfigurationView>(`/api/demo/configuration/profile/${profile}`, {method: "POST"})
};
