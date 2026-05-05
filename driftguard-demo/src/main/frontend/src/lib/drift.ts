import type { DriftEvent, MetricKey, MetricPoint } from "../types";
import { formatNumber } from "./format";

export interface DriftIncident {
  id: string;
  service: string;
  metric: string;
  operation?: string;
  detector: string;
  severity: string;
  startedAt: string;
  recoveredAt?: string;
  explanation: string;
}

export interface DriftEventEvidence {
  label: string;
  value: string;
}

export function streamId(key: MetricKey) {
  return `${key.service}|${key.metric}|${key.operation ?? ""}`;
}

export function groupStreams(points: MetricPoint[]) {
  const byStream = new Map<string, { id: string; service: string; metric: string; operation?: string; points: MetricPoint[] }>();
  for (const point of points) {
    const id = streamId(point.key);
    if (!byStream.has(id)) {
      byStream.set(id, {
        id,
        service: point.key.service,
        metric: point.key.metric,
        operation: point.key.operation,
        points: []
      });
    }
    byStream.get(id)!.points.push(point);
  }
  return [...byStream.values()].sort((left, right) => left.id.localeCompare(right.id));
}

export function eventMatchesQuery(event: DriftEvent, query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return true;
  }

  return [
    event.id,
    event.key.service,
    event.key.metric,
    event.key.instance,
    event.key.operation,
    event.detector,
    event.algorithm,
    event.phase,
    event.severity,
    event.reason,
    eventExplanation(event)
  ]
    .filter(Boolean)
    .some((value) => String(value).toLowerCase().includes(normalized));
}

export function countSeverity(events: DriftEvent[], severity: string) {
  return events.filter((event) => event.severity === severity).length;
}

export function buildIncidents(events: DriftEvent[]) {
  const incidents = new Map<string, DriftIncident>();

  for (const event of events.slice().sort((left, right) => left.detectedAt.localeCompare(right.detectedAt))) {
    const id = incidentId(event);
    const current = incidents.get(id);

    if (event.phase === "RECOVERED") {
      if (current) {
        incidents.set(id, { ...current, recoveredAt: event.detectedAt });
      } else {
        incidents.set(id, eventToIncident(event, event.detectedAt));
      }
      continue;
    }

    if (!current) {
      incidents.set(id, eventToIncident(event));
      continue;
    }

    if (severityRank(event.severity) > severityRank(current.severity)) {
      incidents.set(id, {
        ...current,
        severity: event.severity,
        explanation: eventExplanation(event)
      });
    }
  }

  return [...incidents.values()].sort((left, right) => right.startedAt.localeCompare(left.startedAt));
}

export function eventExplanation(event: DriftEvent) {
  const current = detailNumber(event, "currentMean") ?? event.currentValue;
  const baseline = detailNumber(event, "baselineMean") ?? event.baselineValue;
  const relative = detailNumber(event, "relativeChangePercent");
  const pValue = detailNumber(event, "pValue");
  const statistic = detailNumber(event, "statistic") ?? detailNumber(event, "chiSquare");
  const threshold = event.severity === "CRITICAL"
    ? detailNumber(event, "criticalThreshold")
    : detailNumber(event, "warningThreshold");

  const lifecycle = lifecycleExplanation(event);
  const parts = [
    lifecycle,
    `Current ${formatNumber(current)} vs baseline ${formatNumber(baseline)}`,
    relative === undefined ? undefined : `${relative >= 0 ? "+" : ""}${formatNumber(relative)}%`,
    threshold === undefined ? undefined : `threshold ${formatNumber(threshold)}`,
    pValue === undefined ? undefined : `p-value ${formatNumber(pValue)}`,
    statistic === undefined ? undefined : `statistic ${formatNumber(statistic)}`
  ].filter(Boolean);

  return `${parts.join(" · ")}. ${event.reason}`;
}

export function eventEvidence(event: DriftEvent): DriftEventEvidence[] {
  const relative = detailNumber(event, "relativeChangePercent");
  const pValue = detailNumber(event, "pValue");
  const statistic = detailNumber(event, "statistic") ?? detailNumber(event, "chiSquare");
  const threshold = event.severity === "CRITICAL"
    ? detailNumber(event, "criticalThreshold")
    : detailNumber(event, "warningThreshold");
  const consecutiveSignals = detailNumber(event, "consecutiveSignals");
  const recoveryConsecutiveNormal = detailNumber(event, "recoveryConsecutiveNormal");

  return [
    { label: "Phase", value: event.phase },
    { label: "Direction", value: event.direction },
    { label: "Score", value: formatNumber(event.score) },
    threshold === undefined ? undefined : { label: "Threshold", value: formatNumber(threshold) },
    relative === undefined ? undefined : { label: "Change", value: `${relative >= 0 ? "+" : ""}${formatNumber(relative)}%` },
    pValue === undefined ? undefined : { label: "p-value", value: formatNumber(pValue) },
    statistic === undefined ? undefined : { label: "Statistic", value: formatNumber(statistic) },
    consecutiveSignals === undefined ? undefined : { label: "Signals", value: formatNumber(consecutiveSignals) },
    recoveryConsecutiveNormal === undefined ? undefined : { label: "Recovery normals", value: formatNumber(recoveryConsecutiveNormal) }
  ].filter((item): item is DriftEventEvidence => Boolean(item));
}

function eventToIncident(event: DriftEvent, recoveredAt?: string): DriftIncident {
  return {
    id: incidentId(event),
    service: event.key.service,
    metric: event.key.metric,
    operation: event.key.operation,
    detector: event.detector,
    severity: event.severity,
    startedAt: event.detectedAt,
    recoveredAt,
    explanation: eventExplanation(event)
  };
}

function incidentId(event: DriftEvent) {
  return `${streamId(event.key)}|${event.detector}`;
}

function severityRank(severity: string) {
  return severity === "CRITICAL" ? 3 : severity === "WARNING" ? 2 : severity === "INFO" ? 1 : 0;
}

function lifecycleExplanation(event: DriftEvent) {
  if (event.phase === "RECOVERED") {
    const normals = detailNumber(event, "recoveryConsecutiveNormal");
    return normals === undefined
      ? "Episode recovered near baseline"
      : `Episode recovered after ${formatNumber(normals)} normal observations`;
  }
  if (event.phase === "ONGOING") {
    const signals = detailNumber(event, "consecutiveSignals");
    return signals === undefined
      ? "Episode remains active"
      : `Episode remains active after ${formatNumber(signals)} consecutive drift signals`;
  }
  return "New drift episode started";
}

function detailNumber(event: DriftEvent, key: string) {
  const value = event.details?.[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}
