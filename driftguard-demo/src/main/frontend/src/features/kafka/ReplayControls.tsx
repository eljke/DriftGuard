import type { DemoScenarioRequest } from "../../types";
import { useI18n } from "../../i18n";

export function ReplayControls({
  disabled,
  profiles,
  resetState,
  scenarioParams,
  selectedProfile,
  speed,
  onProfileChange,
  onResetStateChange,
  onScenarioParamsChange,
  onSpeedChange
}: {
  disabled: boolean;
  profiles: string[];
  resetState: boolean;
  scenarioParams: Required<DemoScenarioRequest>;
  selectedProfile: string;
  speed: number;
  onProfileChange: (profile: string) => void;
  onResetStateChange: (reset: boolean) => void;
  onScenarioParamsChange: (params: Required<DemoScenarioRequest>) => void;
  onSpeedChange: (speed: number) => void;
}) {
  const { t } = useI18n();
  const setParam = (key: keyof Required<DemoScenarioRequest>, next: number) => {
    onScenarioParamsChange({ ...scenarioParams, [key]: next });
  };

  return (
    <div className="replay-controls">
      <label className="field">
        <span>{t("replay.speed")}</span>
        <select
          disabled={disabled}
          value={speed}
          onChange={(event) => onSpeedChange(Number(event.target.value))}
        >
          <option value={0.5}>0.5x</option>
          <option value={1}>1x</option>
          <option value={2}>2x</option>
          <option value={5}>5x</option>
          <option value={10}>10x</option>
        </select>
      </label>

      <NumberField disabled={disabled} label={t("synthetic.samplePoints")} max={2000} min={80} step={10} value={scenarioParams.samples} onChange={(next) => setParam("samples", next)} />

      <label className="field">
        <span>{t("replay.profile")}</span>
        <select
          disabled={disabled || profiles.length === 0}
          value={selectedProfile}
          onChange={(event) => onProfileChange(event.target.value)}
        >
          <option value="">{t("replay.currentProfile")}</option>
          {profiles.map((profile) => (
            <option key={profile} value={profile}>{profile}</option>
          ))}
        </select>
      </label>

      <label className="checkbox-field">
        <input
          checked={resetState}
          disabled={disabled}
          type="checkbox"
          onChange={(event) => onResetStateChange(event.target.checked)}
        />
        <span>{t("replay.reset")}</span>
      </label>

      <NumberField disabled={disabled} label={t("synthetic.baselineValue")} min={0} step={1} value={scenarioParams.baselineValue} onChange={(next) => setParam("baselineValue", next)} />
      <NumberField disabled={disabled} label={t("synthetic.driftValue")} min={0} step={1} value={scenarioParams.driftValue} onChange={(next) => setParam("driftValue", next)} />
      <NumberField disabled={disabled} label={t("synthetic.noise")} min={0} step={0.1} value={scenarioParams.noiseStdDev} onChange={(next) => setParam("noiseStdDev", next)} />
      <NumberField disabled={disabled} label={t("synthetic.driftStart")} max={95} min={5} step={1} value={scenarioParams.driftStartPercent} onChange={(next) => setParam("driftStartPercent", next)} />
      <NumberField disabled={disabled} label={t("synthetic.spikeLength")} max={95} min={5} step={1} value={scenarioParams.spikeLengthPercent} onChange={(next) => setParam("spikeLengthPercent", next)} />

      <p className="help-text">
        {t("replay.help")}
      </p>
    </div>
  );
}

function NumberField({
  disabled,
  label,
  max,
  min,
  step,
  value,
  onChange
}: {
  disabled: boolean;
  label: string;
  max?: number;
  min: number;
  step: number;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input
        disabled={disabled}
        max={max}
        min={min}
        step={step}
        type="number"
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
      />
    </label>
  );
}
