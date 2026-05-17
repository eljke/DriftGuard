import { Loader2, Square } from "lucide-react";
import { Notice, Panel } from "../../components/ui";
import { useI18n } from "../../i18n";
import { readableError } from "../../lib/format";
import type { DemoScenarioDescriptor, DemoScenarioRequest, KafkaDemoStatus } from "../../types";
import { ScenarioButtons } from "../common/ScenarioButtons";
import { ReplayControls } from "./ReplayControls";

interface KafkaScenarioPanelProps {
  busy: boolean;
  error: unknown;
  profiles: string[];
  replayProfile: string;
  replaySpeed: number;
  resetState: boolean;
  scenarioParams: Required<DemoScenarioRequest>;
  scenarios: DemoScenarioDescriptor[];
  status?: KafkaDemoStatus;
  stopping: boolean;
  onProfileChange: (profile: string) => void;
  onReplay: (scenario: string) => void;
  onResetStateChange: (reset: boolean) => void;
  onScenarioParamsChange: (params: Required<DemoScenarioRequest>) => void;
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
  scenarioParams,
  scenarios,
  status,
  stopping,
  onProfileChange,
  onReplay,
  onResetStateChange,
  onScenarioParamsChange,
  onRun,
  onSpeedChange,
  onStop
}: KafkaScenarioPanelProps) {
  const { t } = useI18n();

  return (
    <Panel title={t("kafka.runScenario")} className="control-panel">
      {busy && <Notice tone="info" text={t("kafka.busy")} />}
      {status?.error && <Notice tone="error" text={status.error} />}
      {error ? <Notice tone="error" text={readableError(error)} /> : null}
      <ReplayControls
        disabled={busy || Boolean(status?.running)}
        profiles={profiles}
        resetState={resetState}
        scenarioParams={scenarioParams}
        selectedProfile={replayProfile}
        speed={replaySpeed}
        onProfileChange={onProfileChange}
        onResetStateChange={onResetStateChange}
        onScenarioParamsChange={onScenarioParamsChange}
        onSpeedChange={onSpeedChange}
      />
      <ScenarioButtons
        scenarios={scenarios}
        busy={busy || Boolean(status?.running)}
        runLabel={t("kafka.liveRun")}
        onRun={onRun}
        onReplay={onReplay}
      />
      <div className="actions">
        <button className="secondary-button" disabled={!status?.running || stopping} onClick={onStop} type="button">
          {stopping ? <Loader2 className="spin" size={16} /> : <Square size={16} />}
          {t("kafka.stop")}
        </button>
      </div>
    </Panel>
  );
}
