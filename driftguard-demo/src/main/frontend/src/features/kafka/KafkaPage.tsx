import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2, Square } from "lucide-react";
import { useState } from "react";
import { api } from "../../api/client";
import { Notice, Panel } from "../../components/ui";
import { EventsTable } from "../events/EventsTable";
import { IncidentsPanel } from "../events/IncidentsPanel";
import { ScenarioButtons } from "../common/ScenarioButtons";
import { StreamGrid } from "../common/StreamGrid";
import type { DemoConfigurationView, DemoScenarioDescriptor, KafkaDemoStatus, KafkaOperationsSnapshot } from "../../types";
import { readableError } from "../../lib/format";
import { KafkaOperationsPanel } from "./KafkaOperationsPanel";
import { ProducerStrip } from "./ProducerStrip";
import { ReplayControls } from "./ReplayControls";

export function KafkaPage({
  status,
  operations,
  scenarios,
  configuration
}: {
  status?: KafkaDemoStatus;
  operations?: KafkaOperationsSnapshot;
  scenarios: DemoScenarioDescriptor[];
  configuration?: DemoConfigurationView;
}) {
  const queryClient = useQueryClient();
  const [replaySpeed, setReplaySpeed] = useState(2);
  const [replayProfile, setReplayProfile] = useState("");
  const [resetState, setResetState] = useState(true);
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
  const kafkaScenarios = scenarios.filter((scenario) => scenario.id !== "seasonal-latency");
  const busy = start.isPending || replay.isPending || stop.isPending;
  const error = start.error ?? replay.error ?? stop.error;
  const profiles = configuration?.availableProfiles ?? [];

  return (
    <section className="stack kafka-page">
      <KafkaOperationsPanel status={status} operations={operations} />

      <Panel title="Run Kafka scenario" className="control-panel">
        {busy && <Notice tone="info" text="Kafka demo запускается. Создаются topic-и, topology, producer и consumer." />}
        {status?.error && <Notice tone="error" text={status.error} />}
        {error && <Notice tone="error" text={readableError(error)} />}
        <ReplayControls
          disabled={busy || Boolean(status?.running)}
          profiles={profiles}
          resetState={resetState}
          selectedProfile={replayProfile}
          speed={replaySpeed}
          onProfileChange={setReplayProfile}
          onResetStateChange={setResetState}
          onSpeedChange={setReplaySpeed}
        />
        <ScenarioButtons
          scenarios={kafkaScenarios}
          busy={busy || Boolean(status?.running)}
          onRun={(id) => start.mutate(id)}
          onReplay={(id) => replay.mutate({
            scenario: id,
            speed: replaySpeed,
            resetState,
            profile: replayProfile || undefined
          })}
        />
        <div className="actions">
          <button className="secondary-button" disabled={!status?.running || stop.isPending} onClick={() => stop.mutate()} type="button">
            {stop.isPending ? <Loader2 className="spin" size={16} /> : <Square size={16} />}
            Stop Kafka demo
          </button>
        </div>
      </Panel>

      <ProducerStrip status={status} />
      <Panel title="Kafka metric streams">
        <StreamGrid points={status?.samplePoints ?? []} events={status?.consumedEvents ?? []} running={Boolean(status?.running)} />
      </Panel>
      <IncidentsPanel events={status?.consumedEvents ?? []} />
      <EventsTable events={status?.consumedEvents ?? []} />
    </section>
  );
}
