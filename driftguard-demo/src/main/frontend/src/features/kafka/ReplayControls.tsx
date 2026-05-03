export function ReplayControls({
  disabled,
  profiles,
  resetState,
  selectedProfile,
  speed,
  onProfileChange,
  onResetStateChange,
  onSpeedChange
}: {
  disabled: boolean;
  profiles: string[];
  resetState: boolean;
  selectedProfile: string;
  speed: number;
  onProfileChange: (profile: string) => void;
  onResetStateChange: (reset: boolean) => void;
  onSpeedChange: (speed: number) => void;
}) {
  return (
    <div className="replay-controls">
      <label className="field">
        <span>Replay speed</span>
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

      <label className="field">
        <span>Detector profile</span>
        <select
          disabled={disabled || profiles.length === 0}
          value={selectedProfile}
          onChange={(event) => onProfileChange(event.target.value)}
        >
          <option value="">Current profile</option>
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
        <span>Reset detector state before replay</span>
      </label>

      <p className="help-text">
        Replay переигрывает тот же synthetic scenario через Kafka. Это удобно для сравнения профилей и скорости обнаружения на одинаковом потоке.
      </p>
    </div>
  );
}
