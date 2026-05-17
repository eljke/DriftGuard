import type { DemoStoredDriftEvent } from "../../types";
import { useI18n } from "../../i18n";
import { eventExplanation } from "../../lib/drift";
import { formatMoscow } from "../../lib/format";

export function StoredEventsTable({ storedEvents }: { storedEvents: DemoStoredDriftEvent[] }) {
  const { locale, t } = useI18n();

  if (storedEvents.length === 0) {
    return <div className="empty-state compact">{t("events.storedEmpty")}</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>{t("events.received")}</th>
            <th>{t("events.source")}</th>
            <th>{t("events.run")}</th>
            <th>{t("events.severity")}</th>
            <th>{t("events.phase")}</th>
            <th>{t("events.service")}</th>
            <th>{t("events.metric")}</th>
            <th>{t("events.detector")}</th>
            <th>{t("events.explanation")}</th>
          </tr>
        </thead>
        <tbody>
          {storedEvents.map((stored) => {
            const event = stored.event;
            return (
              <tr key={`${stored.source}-${stored.runId}-${event.id}`}>
                <td>{formatMoscow(stored.receivedAt)}</td>
                <td><span className="badge">{stored.source}</span></td>
                <td>{stored.runId}</td>
                <td><span className={`severity ${event.severity.toLowerCase()}`}>{event.severity}</span></td>
                <td><span className={`phase ${event.phase.toLowerCase()}`}>{event.phase}</span></td>
                <td>{event.key.service}</td>
                <td>{event.key.metric}</td>
                <td>{event.detector}</td>
                <td className="event-explanation">{eventExplanation(event, locale)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
