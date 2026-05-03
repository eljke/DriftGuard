import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Square } from "lucide-react";
import { api } from "../api/client";
import { Notice, Panel } from "../components/ui";
import { BenchmarkPanel } from "../features/common/BenchmarkPanel";
import { ScenarioButtons } from "../features/common/ScenarioButtons";
import { ScenarioSummary } from "../features/common/ScenarioSummary";
import { StreamGrid } from "../features/common/StreamGrid";
import { EventsTable } from "../features/events/EventsTable";
import { IncidentsPanel } from "../features/events/IncidentsPanel";
import { readableError } from "../lib/format";
import type { DemoRunResult, DemoScenarioDescriptor } from "../types";

export function SyntheticPage({ result, scenarios }: { result?: DemoRunResult; scenarios: DemoScenarioDescriptor[] }) {
  const queryClient = useQueryClient();
  const run = useMutation({
    mutationFn: api.runScenario,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const live = useMutation({
    mutationFn: api.startLive,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const stopLive = useMutation({
    mutationFn: api.stopLive,
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const benchmark = useQuery({ queryKey: ["benchmark"], queryFn: api.benchmark, enabled: false });
  const profileBenchmark = useQuery({ queryKey: ["profile-benchmark"], queryFn: api.benchmarkProfiles, enabled: false });
  const busy = run.isPending || live.isPending || stopLive.isPending;
  const error = run.error ?? live.error ?? stopLive.error;

  return (
    <section className="stack">
      <Panel title="Synthetic scenarios">
        {busy && <Notice tone="info" text="Команда выполняется. Результат обновится автоматически." />}
        {error && <Notice tone="error" text={readableError(error)} />}
        <ScenarioButtons scenarios={scenarios} busy={busy} onRun={(id) => run.mutate(id)} onLive={(id) => live.mutate(id)} />
      </Panel>
      <BenchmarkPanel
        benchmark={benchmark.data}
        profileBenchmark={profileBenchmark.data ?? []}
        loading={benchmark.isFetching}
        profileLoading={profileBenchmark.isFetching}
        onRun={() => benchmark.refetch()}
        onCompareProfiles={() => profileBenchmark.refetch()}
      />
      <Panel title="Result">
        <ScenarioSummary result={result} />
        {result?.running && <Notice tone="info" text="Live playback активен: точки и события появляются по мере обработки." />}
        <div className="actions">
          <button className="secondary-button" disabled={!result?.running || stopLive.isPending} onClick={() => stopLive.mutate()} type="button">
            <Square size={16} />
            Stop live
          </button>
        </div>
      </Panel>
      <Panel title="Synthetic chart">
        <StreamGrid points={result?.samplePoints ?? []} events={result?.events ?? []} running={Boolean(result?.running)} />
      </Panel>
      <IncidentsPanel events={result?.events ?? []} />
      <EventsTable events={result?.events ?? []} />
    </section>
  );
}
