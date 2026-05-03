import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { api } from "../api/client";
import { MetricCard, Notice, Panel } from "../components/ui";
import { countSeverity } from "../lib/drift";
import { readableError } from "../lib/format";
import { CapabilitiesPanel } from "../features/capabilities/CapabilitiesPanel";
import { ScenarioSummary } from "../features/common/ScenarioSummary";
import { StreamGrid } from "../features/common/StreamGrid";
import { StoredEventsTable } from "../features/events/StoredEventsTable";
import type { DemoCapabilityGroup, DemoRunResult, DemoStoredDriftEvent, KafkaDemoStatus } from "../types";

export function OverviewPage({
  result,
  kafka,
  storedEvents,
  capabilities
}: {
  result?: DemoRunResult;
  kafka?: KafkaDemoStatus;
  storedEvents: DemoStoredDriftEvent[];
  capabilities: DemoCapabilityGroup[];
}) {
  const queryClient = useQueryClient();
  const critical = countSeverity([...(result?.events ?? []), ...(kafka?.consumedEvents ?? [])], "CRITICAL");
  const clearStoredEvents = useMutation({
    mutationFn: api.clearStoredEvents,
    onSuccess: () => queryClient.setQueryData(["stored-events"], [])
  });

  return (
    <section className="page-grid">
      <MetricCard title="Synthetic events" value={result?.events.length ?? 0} helper={result?.title ?? "Нет данных"} />
      <MetricCard title="Kafka events" value={kafka?.consumedEvents.length ?? 0} helper={kafka?.scenario ?? "Нет данных"} />
      <MetricCard title="Critical events" value={critical} helper="По всем demo-источникам" tone="danger" />
      <MetricCard
        title="Kafka progress"
        value={`${kafka?.producedPoints ?? 0}/${kafka?.totalPoints ?? 0}`}
        helper={kafka?.inputTopic ?? "Topic не загружен"}
      />
      <CapabilitiesPanel groups={capabilities} />
      <Panel className="wide" title="Последний synthetic запуск">
        <ScenarioSummary result={result} />
      </Panel>
      <Panel className="wide" title="Kafka streams">
        <StreamGrid points={kafka?.samplePoints ?? []} events={kafka?.consumedEvents ?? []} running={Boolean(kafka?.running)} />
      </Panel>
      <Panel className="wide" title="Recent stored drift events">
        <div className="panel-toolbar">
          <span className="help-text">Общий recent stream из synthetic, live и Kafka demo.</span>
          <button
            className="secondary-button"
            disabled={storedEvents.length === 0 || clearStoredEvents.isPending}
            onClick={() => clearStoredEvents.mutate()}
            type="button"
          >
            {clearStoredEvents.isPending ? <Loader2 className="spin" size={16} /> : null}
            Clear stored events
          </button>
        </div>
        {clearStoredEvents.error && <Notice tone="error" text={readableError(clearStoredEvents.error)} />}
        <StoredEventsTable storedEvents={storedEvents} />
      </Panel>
    </section>
  );
}
