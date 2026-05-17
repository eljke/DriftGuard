import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Square } from "lucide-react";
import { useState } from "react";
import { api } from "../api/client";
import { Notice, Panel } from "../components/ui";
import { BenchmarkPanel } from "../features/common/BenchmarkPanel";
import { ScenarioButtons } from "../features/common/ScenarioButtons";
import { ScenarioSummary } from "../features/common/ScenarioSummary";
import { StreamGrid } from "../features/common/StreamGrid";
import { EventsTable } from "../features/events/EventsTable";
import { IncidentsPanel } from "../features/events/IncidentsPanel";
import { useI18n } from "../i18n";
import { readableError } from "../lib/format";
import type { DemoRunResult, DemoScenarioDescriptor, DemoScenarioRequest } from "../types";

export function SyntheticPage({ result, scenarios }: { result?: DemoRunResult; scenarios: DemoScenarioDescriptor[] }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const [scenarioParams, setScenarioParams] = useState<Required<DemoScenarioRequest>>({
    samples: 160,
    baselineValue: 0,
    driftValue: 0,
    noiseStdDev: 0,
    driftStartPercent: 0,
    spikeLengthPercent: 0
  });
  const request = compactScenarioRequest(scenarioParams);
  const run = useMutation({
    mutationFn: (scenario: string) => api.runScenario(scenario, request),
    onSuccess: (data) => queryClient.setQueryData(["overview"], data)
  });
  const live = useMutation({
    mutationFn: (scenario: string) => api.startLive(scenario, request),
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
      <Panel title={t("synthetic.title")}>
        {busy && <Notice tone="info" text={t("synthetic.busy")} />}
        {error && <Notice tone="error" text={readableError(error)} />}
        <ScenarioLabControls disabled={busy} value={scenarioParams} onChange={setScenarioParams} />
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
      <Panel title={t("synthetic.result")}>
        <ScenarioSummary result={result} />
        {result?.running && <Notice tone="info" text={t("synthetic.liveActive")} />}
        <div className="actions">
          <button className="secondary-button" disabled={!result?.running || stopLive.isPending} onClick={() => stopLive.mutate()} type="button">
            <Square size={16} />
            {t("synthetic.stopLive")}
          </button>
        </div>
      </Panel>
      <Panel title={t("synthetic.chart")}>
        <StreamGrid points={result?.samplePoints ?? []} events={result?.events ?? []} running={Boolean(result?.running)} />
      </Panel>
      <IncidentsPanel events={result?.events ?? []} />
      <EventsTable events={result?.events ?? []} />
    </section>
  );
}

function ScenarioLabControls({
  disabled,
  value,
  onChange
}: {
  disabled: boolean;
  value: Required<DemoScenarioRequest>;
  onChange: (value: Required<DemoScenarioRequest>) => void;
}) {
  const { t } = useI18n();
  const set = (key: keyof Required<DemoScenarioRequest>, next: number) => onChange({ ...value, [key]: next });

  return (
    <div className="scenario-parameters advanced">
      <NumberField disabled={disabled} label={t("synthetic.samplePoints")} max={2000} min={80} step={10} value={value.samples} onChange={(next) => set("samples", next)} />
      <NumberField disabled={disabled} label={t("synthetic.baselineValue")} max={1000000} min={0} step={1} value={value.baselineValue} onChange={(next) => set("baselineValue", next)} />
      <NumberField disabled={disabled} label={t("synthetic.driftValue")} max={1000000} min={0} step={1} value={value.driftValue} onChange={(next) => set("driftValue", next)} />
      <NumberField disabled={disabled} label={t("synthetic.noise")} max={10000} min={0} step={0.1} value={value.noiseStdDev} onChange={(next) => set("noiseStdDev", next)} />
      <NumberField disabled={disabled} label={t("synthetic.driftStart")} max={95} min={5} step={1} value={value.driftStartPercent} onChange={(next) => set("driftStartPercent", next)} />
      <NumberField disabled={disabled} label={t("synthetic.spikeLength")} max={80} min={5} step={1} value={value.spikeLengthPercent} onChange={(next) => set("spikeLengthPercent", next)} />
      <p className="help-text">{t("synthetic.defaultsHelp")}</p>
    </div>
  );
}

function NumberField({
  disabled,
  label,
  max,
  min,
  step,
  value,
  onChange
}: {
  disabled: boolean;
  label: string;
  max: number;
  min: number;
  step: number;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input disabled={disabled} max={max} min={min} step={step} type="number" value={value} onChange={(event) => onChange(Number(event.target.value))} />
    </label>
  );
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
