import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "../../api/client";
import type { KafkaReplayRequest } from "../../types";

export function useKafkaDemo() {
  const queryClient = useQueryClient();
  const [replaySpeed, setReplaySpeed] = useState(2);
  const [replayProfile, setReplayProfile] = useState("");
  const [resetState, setResetState] = useState(true);
  const [samples, setSamples] = useState(160);

  const start = useMutation({
    mutationFn: api.startKafka,
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
      samples
    };
    replay.mutate(request);
  };

  return {
    replaySpeed,
    replayProfile,
    resetState,
    samples,
    setReplaySpeed,
    setReplayProfile,
    setResetState,
    setSamples,
    startScenario: start.mutate,
    replayScenario,
    stop: stop.mutate,
    busy: start.isPending || replay.isPending || stop.isPending,
    stopping: stop.isPending,
    error: start.error ?? replay.error ?? stop.error
  };
}
