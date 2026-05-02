import type {
  DemoConfigurationView,
  DemoRunResult,
  DemoScenarioDescriptor,
  KafkaDemoStatus,
  ToolLink
} from "../types";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...init?.headers
    },
    ...init
  });
  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || `Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export const api = {
  overview: () => request<DemoRunResult>("/api/demo"),
  scenarios: () => request<DemoScenarioDescriptor[]>("/api/demo/scenarios"),
  runScenario: (scenario: string) => request<DemoRunResult>(`/api/demo/run/${scenario}`, { method: "POST" }),
  startLive: (scenario: string) => request<DemoRunResult>(`/api/demo/live/${scenario}`, { method: "POST" }),
  stopLive: () => request<DemoRunResult>("/api/demo/live/stop", { method: "POST" }),
  kafkaStatus: () => request<KafkaDemoStatus>("/api/demo/kafka"),
  startKafka: (scenario: string) => request<KafkaDemoStatus>(`/api/demo/kafka/start/${scenario}`, { method: "POST" }),
  stopKafka: () => request<KafkaDemoStatus>("/api/demo/kafka/stop", { method: "POST" }),
  tools: () => request<ToolLink[]>("/api/demo/tools"),
  configuration: () => request<DemoConfigurationView>("/api/demo/configuration")
};
