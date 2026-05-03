import { useMemo } from "react";
import { TimeSeriesChart } from "../../components/TimeSeriesChart";
import type { DriftEvent, MetricPoint } from "../../types";
import { groupStreams, streamId } from "../../lib/drift";

export function StreamGrid({ points, events, running = false }: { points: MetricPoint[]; events: DriftEvent[]; running?: boolean }) {
  const groups = useMemo(() => groupStreams(points), [points]);
  if (groups.length === 0) {
    return <div className="empty-state">Метрики пока не опубликованы.</div>;
  }
  return (
    <div className="stream-grid">
      {groups.map((group) => {
        const streamEvents = events.filter((event) => streamId(event.key) === group.id);
        return (
          <article className="stream-card" key={group.id}>
            <div className="stream-head">
              <div>
                <strong>{group.service}</strong>
                <span>{group.metric} · {group.operation || "-"}</span>
              </div>
              <span className="badge">{group.points.length} points · {streamEvents.length} events</span>
            </div>
            {running && streamEvents.length === 0 && (
              <div className="inline-hint">Поток идёт, detector ждёт достаточное окно и подтверждение сигнала.</div>
            )}
            <TimeSeriesChart points={group.points} events={streamEvents} />
          </article>
        );
      })}
    </div>
  );
}
