import { Loader2, Square } from "lucide-react";
import { Notice, Panel } from "../../components/ui";
import { readableError } from "../../lib/format";
import type { DemoScenarioDescriptor, KafkaDemoStatus } from "../../types";
import { ScenarioButtons } from "../common/ScenarioButtons";
import { ReplayControls } from "./ReplayControls";

interface KafkaScenarioPanelProps {
  busy: boolean;
  error: unknown;
  profiles: string[];
  replayProfile: string;
  replaySpeed: number;
  resetState: boolean;
  scenarios: DemoScenarioDescriptor[];
  status?: KafkaDemoStatus;
  stopping: boolean;
  onProfileChange: (profile: string) => void;
  onReplay: (scenario: string) => void;
  onResetStateChange: (reset: boolean) => void;
  onRun: (scenario: string) => void;
  onSpeedChange: (speed: number) => void;
  onStop: () => void;
}

export function KafkaScenarioPanel({
  busy,
  error,
  profiles,
  replayProfile,
  replaySpeed,
  resetState,
  scenarios,
  status,
  stopping,
  onProfileChange,
  onReplay,
  onResetStateChange,
  onRun,
  onSpeedChange,
  onStop
}: KafkaScenarioPanelProps) {
  return (
    <Panel title="Run Kafka scenario" className="control-panel">
      {busy && <Notice tone="info" text="Kafka demo запускается. Создаются topic-и, topology, producer и consumer." />}
      {status?.error && <Notice tone="error" text={status.error} />}
      {error && <Notice tone="error" text={readableError(error)} />}
      <ReplayControls
        disabled={busy || Boolean(status?.running)}
        profiles={profiles}
        resetState={resetState}
        selectedProfile={replayProfile}
        speed={replaySpeed}
        onProfileChange={onProfileChange}
        onResetStateChange={onResetStateChange}
        onSpeedChange={onSpeedChange}
      />
      <ScenarioButtons
        scenarios={scenarios}
        busy={busy || Boolean(status?.running)}
        onRun={onRun}
        onReplay={onReplay}
      />
      <div className="actions">
        <button className="secondary-button" disabled={!status?.running || stopping} onClick={onStop} type="button">
          {stopping ? <Loader2 className="spin" size={16} /> : <Square size={16} />}
          Stop Kafka demo
        </button>
      </div>
    </Panel>
  );
}
