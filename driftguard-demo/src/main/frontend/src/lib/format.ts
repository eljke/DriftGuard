export function formatNumber(value?: number) {
  return value !== undefined && Number.isFinite(value)
    ? new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 3 }).format(value)
    : "—";
}

export function formatPercent(value: number) {
  return `${formatNumber(value * 100)}%`;
}

export function formatMoscow(value: string) {
  return new Intl.DateTimeFormat("ru-RU", {
    timeZone: "Europe/Moscow",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit"
  }).format(new Date(value));
}

export function formatDuration(start: string, end?: string) {
  const endTime = end ? new Date(end).getTime() : Date.now();
  const seconds = Math.max(0, Math.round((endTime - new Date(start).getTime()) / 1000));
  if (seconds < 60) {
    return `${seconds}s`;
  }
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  if (minutes < 60) {
    return `${minutes}m ${rest}s`;
  }
  return `${Math.floor(minutes / 60)}h ${minutes % 60}m`;
}

export function boolCount(value?: boolean) {
  return value ? 1 : 0;
}

export function legacyPrecision(metrics: { falsePositive?: boolean }) {
  return metrics.falsePositive ? 0 : 1;
}

export function legacyRecall(metrics: { missed?: boolean }) {
  return metrics.missed ? 0 : 1;
}

export function readableError(error: unknown) {
  return error instanceof Error ? error.message : "Unknown error";
}
