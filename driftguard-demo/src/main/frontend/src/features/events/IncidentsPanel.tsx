import { Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import type { DriftIncident } from "../../lib/drift";
import { buildIncidents } from "../../lib/drift";
import { formatDuration, formatMoscow } from "../../lib/format";
import type { DriftEvent } from "../../types";

export function IncidentsPanel({ events }: { events: DriftEvent[] }) {
  const { locale, t } = useI18n();
  const incidents = buildIncidents(events, locale);
  const active = incidents.filter((incident) => !incident.recoveredAt);
  const recovered = incidents.filter((incident) => incident.recoveredAt);

  return (
    <Panel title={t("incidents.title")}>
      {incidents.length === 0 ? (
        <div className="empty-state">{t("incidents.empty")}</div>
      ) : (
        <div className="incident-layout">
          <IncidentColumn title={t("incidents.active")} incidents={active} emptyText={t("incidents.activeEmpty")} />
          <IncidentColumn title={t("incidents.recovered")} incidents={recovered} emptyText={t("incidents.recoveredEmpty")} />
        </div>
      )}
    </Panel>
  );
}

function IncidentColumn({ title, incidents, emptyText }: { title: string; incidents: DriftIncident[]; emptyText: string }) {
  const { t } = useI18n();

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
                <dt>{t("events.detector")}</dt>
                <dd>{incident.detector}</dd>
                <dt>{t("incidents.started")}</dt>
                <dd>{formatMoscow(incident.startedAt)}</dd>
                <dt>{t("incidents.duration")}</dt>
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
