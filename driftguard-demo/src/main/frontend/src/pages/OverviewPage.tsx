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
import { useI18n } from "../i18n";
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
  const { t } = useI18n();
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
      <MetricCard title={t("overview.syntheticEvents")} value={result?.events.length ?? 0} helper={result?.title ?? t("common.noData")} />
      <MetricCard title={t("overview.kafkaEvents")} value={kafka?.consumedEvents.length ?? 0} helper={kafka?.scenario ?? t("common.noData")} />
      <MetricCard title={t("overview.criticalEvents")} value={critical} helper={t("overview.criticalHelper")} tone="danger" />
      <MetricCard
        title={t("overview.kafkaProgress")}
        value={`${kafka?.producedPoints ?? 0}/${kafka?.totalPoints ?? 0}`}
        helper={kafka?.inputTopic ?? t("overview.topicMissing")}
      />
      <Panel className="wide product-map" title={t("overview.workflow")}>
        <div className="workflow-strip">
          <WorkflowStep icon={<FlaskConical size={18} />} title={t("overview.syntheticStream")} text={t("overview.pointsProcessed", { count: result?.processedPoints ?? 0 })} />
          <WorkflowStep icon={<Settings2 size={18} />} title={t("overview.runtimeProfile")} text={t("overview.runtimeProfileText")} />
          <WorkflowStep icon={<Cable size={18} />} title={t("overview.kafkaTopology")} text={kafka?.running ? t("overview.kafkaActive") : t("overview.kafkaReady")} />
          <WorkflowStep icon={<ShieldCheck size={18} />} title={t("overview.incidents")} text={t("overview.incidentCounts", { stored: storedEvents.length, critical: storedCritical })} />
        </div>
        <div className="product-map-footer">
          <span>{t("overview.visibleStreams", { count: activeStreams })}</span>
          <span>{t("overview.capabilitiesCount", { count: capabilities.reduce((total, group) => total + group.capabilities.length, 0) })}</span>
        </div>
      </Panel>
      <CapabilitiesPanel groups={capabilities} />
      <Panel className="wide" title={t("overview.lastSynthetic")}>
        <ScenarioSummary result={result} />
      </Panel>
      <Panel className="wide" title={t("overview.kafkaStreams")}>
        <StreamGrid points={kafka?.samplePoints ?? []} events={kafka?.consumedEvents ?? []} running={Boolean(kafka?.running)} />
      </Panel>
      <Panel className="wide" title={t("overview.recentStored")}>
        <div className="panel-toolbar">
          <span className="help-text">{t("overview.recentStoredHelp")}</span>
          <button
            className="secondary-button"
            disabled={storedEvents.length === 0 || clearStoredEvents.isPending}
            onClick={() => clearStoredEvents.mutate()}
            type="button"
          >
            {clearStoredEvents.isPending ? <Loader2 className="spin" size={16} /> : null}
            {t("overview.clearStored")}
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
