import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "../../api/client";
import type { DemoScenarioRequest, KafkaReplayRequest } from "../../types";

export function useKafkaDemo() {
  const queryClient = useQueryClient();
  const [replaySpeed, setReplaySpeed] = useState(2);
  const [replayProfile, setReplayProfile] = useState("");
  const [resetState, setResetState] = useState(true);
  const [scenarioParams, setScenarioParams] = useState<Required<DemoScenarioRequest>>({
    samples: 160,
    baselineValue: 0,
    driftValue: 0,
    noiseStdDev: 0,
    driftStartPercent: 0,
    spikeLengthPercent: 0
  });

  const start = useMutation({
    mutationFn: ({ scenario, request }: { scenario: string; request: DemoScenarioRequest }) => api.startKafka(scenario, request),
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });

  const stop = useMutation({
    mutationFn: api.stopKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });

  const replay = useMutation({
    mutationFn: api.replayKafka,
    onSuccess: (data) => queryClient.setQueryData(["kafka"], data)
  });

  const replayScenario = (scenario: string) => {
    const request: KafkaReplayRequest = {
      scenario,
      speed: replaySpeed,
      resetState,
      profile: replayProfile || undefined,
      ...compactScenarioRequest(scenarioParams)
    };
    replay.mutate(request);
  };

  const startScenario = (scenario: string) => {
    start.mutate({ scenario, request: compactScenarioRequest(scenarioParams) });
  };

  return {
    replaySpeed,
    replayProfile,
    resetState,
    scenarioParams,
    setReplaySpeed,
    setReplayProfile,
    setResetState,
    setScenarioParams,
    startScenario,
    replayScenario,
    stop: stop.mutate,
    busy: start.isPending || replay.isPending || stop.isPending,
    stopping: stop.isPending,
    error: start.error ?? replay.error ?? stop.error
  };
}

function compactScenarioRequest(value: Required<DemoScenarioRequest>): DemoScenarioRequest {
  return {
    samples: value.samples,
    baselineValue: value.baselineValue || undefined,
    driftValue: value.driftValue || undefined,
    noiseStdDev: value.noiseStdDev || undefined,
    driftStartPercent: value.driftStartPercent || undefined,
    spikeLengthPercent: value.spikeLengthPercent || undefined
  };
}
