import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Cable, FlaskConical, Loader2, Settings2, ShieldCheck } from "lucide-react";
import type { ReactNode } from "react";
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
  const activeStreams = new Set(
    [...(result?.samplePoints ?? []), ...(kafka?.samplePoints ?? [])].map((point) => `${point.key.service}|${point.key.metric}|${point.key.operation ?? ""}`)
  ).size;
  const storedCritical = storedEvents.filter(({ event }) => event.severity === "CRITICAL").length;
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
      <Panel className="wide product-map" title="Product workflow">
        <div className="workflow-strip">
          <WorkflowStep icon={<FlaskConical size={18} />} title="Synthetic stream" text={`${result?.processedPoints ?? 0} points processed`} />
          <WorkflowStep icon={<Settings2 size={18} />} title="Runtime profile" text="Detector sensitivity can be changed without restarting UI" />
          <WorkflowStep icon={<Cable size={18} />} title="Kafka topology" text={kafka?.running ? "Producer, Streams and consumer are active" : "Ready for stateful replay"} />
          <WorkflowStep icon={<ShieldCheck size={18} />} title="Incidents" text={`${storedEvents.length} stored, ${storedCritical} critical`} />
        </div>
        <div className="product-map-footer">
          <span><strong>{activeStreams}</strong> metric streams visible across synthetic and Kafka demos</span>
          <span><strong>{capabilities.reduce((total, group) => total + group.capabilities.length, 0)}</strong> backend capabilities mapped to UI/API surfaces</span>
        </div>
      </Panel>
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

function WorkflowStep({ icon, title, text }: { icon: ReactNode; title: string; text: string }) {
  return (
    <article className="workflow-step">
      <span className="workflow-icon">{icon}</span>
      <div>
        <strong>{title}</strong>
        <span>{text}</span>
      </div>
    </article>
  );
}
