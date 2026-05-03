import type { DemoStoredDriftEvent } from "../../types";
import { eventExplanation } from "../../lib/drift";
import { formatMoscow } from "../../lib/format";

export function StoredEventsTable({ storedEvents }: { storedEvents: DemoStoredDriftEvent[] }) {
  if (storedEvents.length === 0) {
    return <div className="empty-state compact">Сохранённых drift events пока нет.</div>;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Received MSK</th>
            <th>Source</th>
            <th>Run</th>
            <th>Severity</th>
            <th>Phase</th>
            <th>Service</th>
            <th>Metric</th>
            <th>Detector</th>
            <th>Explanation</th>
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
                <td className="event-explanation">{eventExplanation(event)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
