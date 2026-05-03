import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { api } from "../api/client";
import { MetricCard, Notice, Panel } from "../components/ui";
import { readableError } from "../lib/format";
import type { DemoConfigurationView } from "../types";

export function ConfigurationPage({ configuration }: { configuration?: DemoConfigurationView }) {
  const queryClient = useQueryClient();
  const updateProfile = useMutation({
    mutationFn: api.updateProfile,
    onSuccess: (data) => queryClient.setQueryData(["configuration"], data)
  });

  if (!configuration) {
    return <Panel title="Configuration">Загрузка конфигурации...</Panel>;
  }

  return (
    <section className="stack">
      <div className="page-grid">
        <MetricCard title="Aggressiveness" value={configuration.aggressiveness.level} helper={configuration.aggressiveness.description} />
        <MetricCard title="Kafka input" value={configuration.kafka.inputTopic} helper={configuration.kafka.bootstrapServers} />
        <MetricCard title="Kafka output" value={configuration.kafka.outputTopic} helper={configuration.kafka.applicationId} />
        <MetricCard title="Playback" value={configuration.kafka.playbackInterval} helper="Интервал публикации точек" />
      </div>
      <Panel title="Registered algorithms">
        <div className="algorithm-list">
          {configuration.registeredAlgorithms.map((algorithm) => (
            <span className="badge" key={algorithm}>{algorithm}</span>
          ))}
        </div>
        <p className="panel-note">
          Starter автоматически объединяет встроенные алгоритмы DriftGuard и пользовательские DetectorAlgorithm bean-ы.
        </p>
      </Panel>
      <Panel title="Runtime detector profile">
        {updateProfile.isPending && <Notice tone="info" text="Профиль применяется: engine пересоздаётся, состояние detector-ов сбрасывается." />}
        {updateProfile.error && <Notice tone="error" text={readableError(updateProfile.error)} />}
        <div className="actions">
          {configuration.availableProfiles.map((profile) => {
            const active = configuration.aggressiveness.level.toUpperCase() === profile;
            return (
              <button
                className={active ? "primary-button" : "secondary-button"}
                disabled={updateProfile.isPending || active}
                key={profile}
                onClick={() => updateProfile.mutate(profile)}
                type="button"
              >
                {updateProfile.isPending ? <Loader2 className="spin" size={16} /> : null}
                {profile}
              </button>
            );
          })}
        </div>
        <p className="help-text">
          Профиль реально меняет runtime DriftDetectorEngine. Kafka topology не пересобирается: она вызывает runtime.detect(), поэтому следующие сообщения сразу идут через новый engine.
        </p>
      </Panel>
      <Panel title="Detector definitions">
        <div className="detector-grid">
          {configuration.detectors.map((detector) => (
            <article className="detector-card" key={detector.name}>
              <div className="detector-head">
                <strong>{detector.name}</strong>
                <span className="badge">{detector.sensitivity}</span>
              </div>
              <dl>
                <dt>Algorithm</dt>
                <dd>{detector.algorithm}</dd>
                <dt>Metrics</dt>
                <dd>{detector.metrics.join(", ") || "any"}</dd>
                <dt>Warning</dt>
                <dd>{detector.warningThreshold} / p={detector.warningPValue}</dd>
                <dt>Critical</dt>
                <dd>{detector.criticalThreshold} / p={detector.criticalPValue}</dd>
                <dt>Emission</dt>
                <dd>
                  {detector.emissionPolicy.minConsecutiveSignals} signals, {detector.emissionPolicy.cooldown}, recovery {detector.emissionPolicy.recoveryConsecutiveNormal}
                </dd>
              </dl>
            </article>
          ))}
        </div>
      </Panel>
    </section>
  );
}
