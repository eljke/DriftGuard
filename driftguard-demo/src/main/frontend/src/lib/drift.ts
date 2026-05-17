import type { DriftEvent, MetricKey, MetricPoint } from "../types";
import type { Locale } from "../i18n";
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

export function buildIncidents(events: DriftEvent[], locale: Locale = "en") {
  const incidents = new Map<string, DriftIncident>();

  for (const event of events.slice().sort((left, right) => left.detectedAt.localeCompare(right.detectedAt))) {
    const id = incidentId(event);
    const current = incidents.get(id);

    if (event.phase === "RECOVERED") {
      if (current) {
        incidents.set(id, { ...current, recoveredAt: event.detectedAt });
      } else {
        incidents.set(id, eventToIncident(event, locale, event.detectedAt));
      }
      continue;
    }

    if (!current) {
      incidents.set(id, eventToIncident(event, locale));
      continue;
    }

    if (severityRank(event.severity) > severityRank(current.severity)) {
      incidents.set(id, {
        ...current,
        severity: event.severity,
        explanation: eventExplanation(event, locale)
      });
    }
  }

  return [...incidents.values()].sort((left, right) => right.startedAt.localeCompare(left.startedAt));
}

export function eventExplanation(event: DriftEvent, locale: Locale = "en") {
  const current = detailNumber(event, "currentMean") ?? event.currentValue;
  const baseline = detailNumber(event, "baselineMean") ?? event.baselineValue;
  const relative = detailNumber(event, "relativeChangePercent");
  const pValue = detailNumber(event, "pValue");
  const statistic = detailNumber(event, "statistic") ?? detailNumber(event, "chiSquare");
  const threshold = event.severity === "CRITICAL"
    ? detailNumber(event, "criticalThreshold")
    : detailNumber(event, "warningThreshold");

  const lifecycle = lifecycleExplanation(event, locale);
  const parts = [
    lifecycle,
    locale === "ru"
      ? `Текущее ${formatNumber(current)} против baseline ${formatNumber(baseline)}`
      : `Current ${formatNumber(current)} vs baseline ${formatNumber(baseline)}`,
    relative === undefined ? undefined : `${relative >= 0 ? "+" : ""}${formatNumber(relative)}%`,
    threshold === undefined ? undefined : `${locale === "ru" ? "порог" : "threshold"} ${formatNumber(threshold)}`,
    pValue === undefined ? undefined : `p-value ${formatNumber(pValue)}`,
    statistic === undefined ? undefined : `${locale === "ru" ? "статистика" : "statistic"} ${formatNumber(statistic)}`
  ].filter(Boolean);

  return `${parts.join(" · ")}. ${event.reason}`;
}

export function eventEvidence(event: DriftEvent, locale: Locale = "en"): DriftEventEvidence[] {
  const relative = detailNumber(event, "relativeChangePercent");
  const pValue = detailNumber(event, "pValue");
  const statistic = detailNumber(event, "statistic") ?? detailNumber(event, "chiSquare");
  const threshold = event.severity === "CRITICAL"
    ? detailNumber(event, "criticalThreshold")
    : detailNumber(event, "warningThreshold");
  const consecutiveSignals = detailNumber(event, "consecutiveSignals");
  const recoveryConsecutiveNormal = detailNumber(event, "recoveryConsecutiveNormal");

  return [
    { label: locale === "ru" ? "Phase" : "Phase", value: event.phase },
    { label: locale === "ru" ? "Direction" : "Direction", value: event.direction },
    { label: "Score", value: formatNumber(event.score) },
    threshold === undefined ? undefined : { label: locale === "ru" ? "Порог" : "Threshold", value: formatNumber(threshold) },
    relative === undefined ? undefined : { label: locale === "ru" ? "Изменение" : "Change", value: `${relative >= 0 ? "+" : ""}${formatNumber(relative)}%` },
    pValue === undefined ? undefined : { label: "p-value", value: formatNumber(pValue) },
    statistic === undefined ? undefined : { label: locale === "ru" ? "Статистика" : "Statistic", value: formatNumber(statistic) },
    consecutiveSignals === undefined ? undefined : { label: locale === "ru" ? "Сигналы" : "Signals", value: formatNumber(consecutiveSignals) },
    recoveryConsecutiveNormal === undefined ? undefined : { label: locale === "ru" ? "Recovery normals" : "Recovery normals", value: formatNumber(recoveryConsecutiveNormal) }
  ].filter((item): item is DriftEventEvidence => Boolean(item));
}

function eventToIncident(event: DriftEvent, locale: Locale, recoveredAt?: string): DriftIncident {
  return {
    id: incidentId(event),
    service: event.key.service,
    metric: event.key.metric,
    operation: event.key.operation,
    detector: event.detector,
    severity: event.severity,
    startedAt: event.detectedAt,
    recoveredAt,
    explanation: eventExplanation(event, locale)
  };
}

function incidentId(event: DriftEvent) {
  return `${streamId(event.key)}|${event.detector}`;
}

function severityRank(severity: string) {
  return severity === "CRITICAL" ? 3 : severity === "WARNING" ? 2 : severity === "INFO" ? 1 : 0;
}

function lifecycleExplanation(event: DriftEvent, locale: Locale) {
  if (event.phase === "RECOVERED") {
    const normals = detailNumber(event, "recoveryConsecutiveNormal");
    return normals === undefined
      ? (locale === "ru" ? "Episode восстановился около baseline" : "Episode recovered near baseline")
      : (locale === "ru" ? `Episode восстановился после нормальных наблюдений: ${formatNumber(normals)}` : `Episode recovered after ${formatNumber(normals)} normal observations`);
  }
  if (event.phase === "ONGOING") {
    const signals = detailNumber(event, "consecutiveSignals");
    return signals === undefined
      ? (locale === "ru" ? "Episode остаётся активным" : "Episode remains active")
      : (locale === "ru" ? `Episode остаётся активным после drift-сигналов подряд: ${formatNumber(signals)}` : `Episode remains active after ${formatNumber(signals)} consecutive drift signals`);
  }
  return locale === "ru" ? "Начался новый drift episode" : "New drift episode started";
}

function detailNumber(event: DriftEvent, key: string) {
  const value = event.details?.[key];
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}
