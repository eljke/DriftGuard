import { Panel } from "../../components/ui";
import type { DriftIncident } from "../../lib/drift";
import { buildIncidents } from "../../lib/drift";
import { formatDuration, formatMoscow } from "../../lib/format";
import type { DriftEvent } from "../../types";

export function IncidentsPanel({ events }: { events: DriftEvent[] }) {
  const incidents = buildIncidents(events);
  const active = incidents.filter((incident) => !incident.recoveredAt);
  const recovered = incidents.filter((incident) => incident.recoveredAt);

  return (
    <Panel title="Drift incidents">
      {incidents.length === 0 ? (
        <div className="empty-state">Активных или завершённых drift episodes пока нет.</div>
      ) : (
        <div className="incident-layout">
          <IncidentColumn title="Active incidents" incidents={active} emptyText="Активных incident-ов нет." />
          <IncidentColumn title="Recovered incidents" incidents={recovered} emptyText="Завершённых incident-ов нет." />
        </div>
      )}
    </Panel>
  );
}

function IncidentColumn({ title, incidents, emptyText }: { title: string; incidents: DriftIncident[]; emptyText: string }) {
  return (
    <div className="incident-column">
      <h3>{title}</h3>
      {incidents.length === 0 ? (
        <div className="empty-state compact">{emptyText}</div>
      ) : (
        <div className="incident-list">
          {incidents.map((incident) => (
            <article className={incident.recoveredAt ? "incident-card recovered" : "incident-card active"} key={incident.id}>
              <div className="incident-head">
                <div>
                  <strong>{incident.service}</strong>
                  <span>{incident.metric} · {incident.operation || "-"}</span>
                </div>
                <span className={`severity ${incident.severity.toLowerCase()}`}>{incident.severity}</span>
              </div>
              <dl className="incident-meta">
                <dt>Detector</dt>
                <dd>{incident.detector}</dd>
                <dt>Started</dt>
                <dd>{formatMoscow(incident.startedAt)}</dd>
                <dt>Duration</dt>
                <dd>{formatDuration(incident.startedAt, incident.recoveredAt)}</dd>
              </dl>
              <p>{incident.explanation}</p>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
