import { useMemo } from "react";
import { TimeSeriesChart } from "../../components/TimeSeriesChart";
import { useI18n } from "../../i18n";
import type { DriftEvent, MetricPoint } from "../../types";
import { groupStreams, streamId } from "../../lib/drift";

export function StreamGrid({ points, events, running = false }: { points: MetricPoint[]; events: DriftEvent[]; running?: boolean }) {
  const { t } = useI18n();
  const groups = useMemo(() => groupStreams(points), [points]);
  if (groups.length === 0) {
    return <div className="empty-state">{t("stream.empty")}</div>;
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
              <span className="badge">{t("stream.badge", { points: group.points.length, events: streamEvents.length })}</span>
            </div>
            {running && streamEvents.length === 0 && (
              <div className="inline-hint">{t("stream.waiting")}</div>
            )}
            <TimeSeriesChart points={group.points} events={streamEvents} />
          </article>
        );
      })}
    </div>
  );
}
