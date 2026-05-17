import { Activity, ArrowRight, CheckCircle2, Database, RadioTower, RotateCcw, ServerCog, ShieldAlert } from "lucide-react";
import type { ReactNode } from "react";
import { MetricCard, Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import { formatNumber } from "../../lib/format";
import type { DriftEvent, KafkaDemoStatus, KafkaOperationsSnapshot } from "../../types";

export function KafkaOperationsPanel({ status, operations }: { status?: KafkaDemoStatus; operations?: KafkaOperationsSnapshot }) {
  const { t } = useI18n();
  const producerCount = status?.producers.length ?? 0;
  const activeProducers = status?.producers.filter((producer) => producer.running).length ?? 0;
  const progressValue = status?.totalPoints ? Math.round(((status.producedPoints ?? 0) / status.totalPoints) * 100) : 0;
  const events = status?.consumedEvents ?? [];
  const lastEvent = events.at(-1);
  const lifecycle = eventLifecycle(events);
  const hasRecovered = lifecycle.recovered > 0;
  const hasOngoing = lifecycle.ongoing > 0;

  return (
    <Panel title={t("kafka.operations")} className="ops-panel">
      <div className={status?.running ? "ops-hero active" : "ops-hero"}>
        <div>
          <p className="eyebrow">{t("kafka.pipeline")}</p>
          <h2>{status?.running ? t("kafka.running") : t("kafka.idle")}</h2>
          <p>
            {status?.scenario ?? t("kafka.noScenario")} · {status?.replay ? t("scenario.replay") : t("kafka.normal")} · {status?.speed ?? 1}x
          </p>
        </div>
        <div className="ops-progress-ring" aria-label={t("kafka.progress", { value: progressValue })}>
          <strong>{progressValue}%</strong>
          <span>{status?.producedPoints ?? 0}/{status?.totalPoints ?? 0}</span>
        </div>
      </div>

      <PipelineMap status={status} operations={operations} activeProducers={activeProducers} producerCount={producerCount} />

      <div className="ops-kpis">
        <MetricCard title={t("kafka.processed")} value={formatNumber(operations?.metrics.processedPoints)} helper={t("kafka.processedHelper")} />
        <MetricCard title={t("summary.events")} value={formatNumber(operations?.metrics.emittedEvents ?? events.length)} helper={t("kafka.eventsHelper")} />
        <MetricCard
          title={t("kafka.errors")}
          value={`${formatNumber(operations?.metrics.failedPoints)} / ${formatNumber(operations?.metrics.routedErrors)}`}
          helper={t("kafka.errorsHelper")}
          tone={(operations?.metrics.failedPoints ?? 0) > 0 ? "danger" : undefined}
        />
        <MetricCard
          title={t("kafka.latency")}
          value={`${formatNumber(operations?.metrics.meanDurationMillis)} ms`}
          helper={operations?.telemetryEnabled ? t("kafka.telemetry") : t("kafka.telemetryMissing")}
        />
      </div>

      <div className="ops-lifecycle-grid">
        <LifecycleTile icon={<ShieldAlert size={18} />} label={t("kafka.started")} value={lifecycle.started} helper={t("kafka.startedHelper")} />
        <LifecycleTile icon={<Activity size={18} />} label={t("kafka.ongoing")} value={lifecycle.ongoing} helper={hasOngoing ? t("kafka.ongoingActive") : t("kafka.ongoingWaiting")} />
        <LifecycleTile icon={<CheckCircle2 size={18} />} label={t("kafka.recovered")} value={lifecycle.recovered} helper={hasRecovered ? t("kafka.recoveredActive") : t("kafka.recoveredWaiting")} />
        <LifecycleTile icon={<RotateCcw size={18} />} label={t("kafka.active")} value={lifecycle.active} helper={t("kafka.activeHelper")} />
      </div>

      <div className="ops-meta-row">
        <span><strong>{t("kafka.producers")}</strong>{activeProducers}/{producerCount}</span>
        <span><strong>{t("kafka.input")}</strong>{operations?.streamsInputTopics?.join(", ") || status?.inputTopic || "—"}</span>
        <span><strong>{t("kafka.output")}</strong>{operations?.outputTopic || status?.outputTopic || "—"}</span>
        <span><strong>{t("kafka.stateStore")}</strong>{operations?.runtimeStateStoreName ?? "—"}</span>
        <span><strong>{t("kafka.errorMode")}</strong>{operations?.detectionErrorMode ?? "—"}</span>
        <span><strong>{t("kafka.lastEvent")}</strong>{lastEvent ? `${lastEvent.phase} · ${lastEvent.key.metric}` : "—"}</span>
      </div>

      <div className="ops-checklist compact">
        <OperationCheck active={Boolean(status?.enabled)} label={t("kafka.enabled")} detail={status?.outputTopic ?? t("kafka.outputMissing")} />
        <OperationCheck active={Boolean(status?.running)} label={t("kafka.topologyRunning")} detail={status?.inputTopic ?? t("kafka.inputMissing")} />
        <OperationCheck active={Boolean(status?.replay)} label={t("kafka.replayMode")} detail={`speed ${status?.speed ?? 1}x`} />
        <OperationCheck active={!status?.error} label={t("kafka.noRuntimeError")} detail={status?.error ?? t("kafka.noStatusErrors")} />
      </div>
    </Panel>
  );
}

function PipelineMap({
  status,
  operations,
  activeProducers,
  producerCount
}: {
  status?: KafkaDemoStatus;
  operations?: KafkaOperationsSnapshot;
  activeProducers: number;
  producerCount: number;
}) {
  const { t } = useI18n();

  return (
    <div className="pipeline-map" aria-label="Kafka integration pipeline">
      <PipelineStep active={activeProducers > 0} detail={`${activeProducers}/${producerCount} ${t("kafka.active").toLowerCase()}`} icon={<RadioTower size={18} />} label={t("kafka.demoProducers")} />
      <PipelineArrow />
      <PipelineStep active={Boolean(status?.running)} detail={operations?.streamsInputTopics?.join(", ") || status?.inputTopic || "-"} icon={<Database size={18} />} label={t("kafka.metricTopic")} />
      <PipelineArrow />
      <PipelineStep active={Boolean(status?.running)} detail={operations?.streamsApplicationId ?? "-"} icon={<ServerCog size={18} />} label={t("kafka.topology")} />
      <PipelineArrow />
      <PipelineStep active={(operations?.metrics.emittedEvents ?? 0) > 0 || (status?.consumedEvents.length ?? 0) > 0} detail={operations?.streamsOutputTopic || status?.outputTopic || "-"} icon={<ShieldAlert size={18} />} label={t("kafka.eventOutput")} />
    </div>
  );
}

function PipelineStep({ active, detail, icon, label }: { active: boolean; detail: string; icon: ReactNode; label: string }) {
  return (
    <div className={active ? "pipeline-step active" : "pipeline-step"}>
      <span>{icon}</span>
      <strong>{label}</strong>
      <small>{detail}</small>
    </div>
  );
}

function PipelineArrow() {
  return (
    <span className="pipeline-arrow" aria-hidden="true">
      <ArrowRight size={16} />
    </span>
  );
}

function LifecycleTile({ icon, label, value, helper }: { icon: ReactNode; label: string; value: number; helper: string }) {
  return (
    <div className="lifecycle-tile">
      <span className="lifecycle-icon">{icon}</span>
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
        <small>{helper}</small>
      </div>
    </div>
  );
}

function OperationCheck({ active, label, detail }: { active: boolean; label: string; detail: string }) {
  return (
    <div className={active ? "operation-check active" : "operation-check"}>
      <span className="operation-dot" />
      <div>
        <strong>{label}</strong>
        <span>{detail}</span>
      </div>
    </div>
  );
}

function eventLifecycle(events: DriftEvent[]) {
  const started = events.filter((event) => event.phase === "STARTED").length;
  const ongoing = events.filter((event) => event.phase === "ONGOING").length;
  const recovered = events.filter((event) => event.phase === "RECOVERED").length;
  const active = new Set<string>();

  for (const event of events) {
    const key = `${event.key.service}|${event.key.metric}|${event.key.operation ?? ""}|${event.detector}`;
    if (event.phase === "RECOVERED") {
      active.delete(key);
    } else {
      active.add(key);
    }
  }

  return { started, ongoing, recovered, active: active.size };
}
