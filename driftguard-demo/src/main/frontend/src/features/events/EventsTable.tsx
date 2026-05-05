import { Search } from "lucide-react";
import { useMemo, useState } from "react";
import { Panel } from "../../components/ui";
import type { DriftEvent } from "../../types";
import { eventEvidence, eventExplanation, eventMatchesQuery } from "../../lib/drift";
import { formatMoscow, formatNumber } from "../../lib/format";

export function EventsTable({ events }: { events: DriftEvent[] }) {
  const [severity, setSeverity] = useState("ALL");
  const [phase, setPhase] = useState("ALL");
  const [query, setQuery] = useState("");
  const visibleEvents = useMemo(
    () => events
      .filter((event) => severity === "ALL" || event.severity === severity)
      .filter((event) => phase === "ALL" || event.phase === phase)
      .filter((event) => eventMatchesQuery(event, query))
      .slice()
      .reverse(),
    [events, phase, query, severity]
  );

  return (
    <Panel title="Drift events">
      {events.length === 0 ? (
        <div className="empty-state">Событий drift-а пока нет.</div>
      ) : (
        <>
          <EventFilters
            phase={phase}
            query={query}
            severity={severity}
            total={events.length}
            visible={visibleEvents.length}
            onPhaseChange={setPhase}
            onQueryChange={setQuery}
            onSeverityChange={setSeverity}
          />
          {visibleEvents.length === 0 ? (
            <div className="empty-state compact">По текущим фильтрам событий нет.</div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Time MSK</th>
                    <th>Event</th>
                    <th>Metric</th>
                    <th>Values</th>
                    <th>Explanation</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleEvents.map((event) => (
                    <tr key={event.id}>
                      <td>{formatMoscow(event.detectedAt)}</td>
                      <td>
                        <div className="event-cell">
                          {event.phase !== "RECOVERED" ? <span className={`severity ${event.severity.toLowerCase()}`}>{event.severity}</span> : null}
                          <span className={`phase ${event.phase.toLowerCase()}`}>{event.phase}</span>
                          <span className="muted-line">{event.algorithm}</span>
                        </div>
                      </td>
                      <td>
                        <div className="event-cell">
                          <strong>{event.key.service}</strong>
                          <span className="muted-line">{event.key.metric} · {event.key.operation ?? "-"}</span>
                        </div>
                      </td>
                      <td><EventValueSummary event={event} /></td>
                      <td className="event-explanation">
                        <EventExplanation event={event} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </>
      )}
    </Panel>
  );
}

function EventFilters({
  phase,
  query,
  severity,
  total,
  visible,
  onPhaseChange,
  onQueryChange,
  onSeverityChange
}: {
  phase: string;
  query: string;
  severity: string;
  total: number;
  visible: number;
  onPhaseChange: (phase: string) => void;
  onQueryChange: (query: string) => void;
  onSeverityChange: (severity: string) => void;
}) {
  return (
    <div className="event-filters">
      <label className="search-field">
        <Search size={16} />
        <input
          placeholder="Search service, metric, detector, algorithm or reason"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
        />
      </label>
      <label className="field compact-field">
        <span>Severity</span>
        <select value={severity} onChange={(event) => onSeverityChange(event.target.value)}>
          <option value="ALL">All</option>
          <option value="INFO">INFO</option>
          <option value="WARNING">WARNING</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
      </label>
      <label className="field compact-field">
        <span>Phase</span>
        <select value={phase} onChange={(event) => onPhaseChange(event.target.value)}>
          <option value="ALL">All</option>
          <option value="STARTED">STARTED</option>
          <option value="ONGOING">ONGOING</option>
          <option value="RECOVERED">RECOVERED</option>
        </select>
      </label>
      <span className="filter-counter">{visible}/{total} events</span>
    </div>
  );
}

function EventExplanation({ event }: { event: DriftEvent }) {
  return (
    <div className="event-explanation-stack">
      <p>{eventExplanation(event)}</p>
      <div className="evidence-list">
        {eventEvidence(event).map((item) => (
          <span className="evidence-pill" key={`${item.label}-${item.value}`}>
            <b>{item.label}</b>
            {item.value}
          </span>
        ))}
      </div>
    </div>
  );
}

function EventValueSummary({ event }: { event: DriftEvent }) {
  return (
    <dl className="event-values">
      <div>
        <dt>Current</dt>
        <dd>{formatNumber(event.currentValue)}</dd>
      </div>
      <div>
        <dt>Baseline</dt>
        <dd>{formatNumber(event.baselineValue)}</dd>
      </div>
      <div>
        <dt>Score</dt>
        <dd>{formatNumber(event.score)}</dd>
      </div>
    </dl>
  );
}
