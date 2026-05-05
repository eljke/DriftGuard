import { Loader2, Play } from "lucide-react";
import type { DemoScenarioDescriptor } from "../../types";

export function ScenarioButtons({
  scenarios,
  busy,
  onRun,
  onReplay,
  onLive
}: {
  scenarios: DemoScenarioDescriptor[];
  busy?: boolean;
  onRun: (scenario: string) => void;
  onReplay?: (scenario: string) => void;
  onLive?: (scenario: string) => void;
}) {
  return (
    <div className="scenario-grid">
      {scenarios.map((scenario) => (
        <article className="scenario-card" key={scenario.id}>
          <div>
            <strong>{scenario.title}</strong>
            <span>{scenario.description}</span>
          </div>
          <div className="scenario-actions">
            <button className="primary-button" disabled={busy} onClick={() => onRun(scenario.id)} type="button">
              {busy ? <Loader2 className="spin" size={16} /> : <Play size={16} />}
              Run
            </button>
            {onReplay && (
              <button className="secondary-button" disabled={busy} onClick={() => onReplay(scenario.id)} type="button">
                Replay
              </button>
            )}
            {onLive && (
              <button className="secondary-button" disabled={busy} onClick={() => onLive(scenario.id)} type="button">
                Live
              </button>
            )}
          </div>
        </article>
      ))}
    </div>
  );
}
