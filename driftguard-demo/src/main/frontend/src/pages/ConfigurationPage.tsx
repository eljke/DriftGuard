import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Loader2 } from "lucide-react";
import { api } from "../api/client";
import { MetricCard, Notice, Panel } from "../components/ui";
import { useI18n } from "../i18n";
import { readableError } from "../lib/format";
import type { DemoConfigurationView } from "../types";

export function ConfigurationPage({ configuration }: { configuration?: DemoConfigurationView }) {
  const { t } = useI18n();
  const queryClient = useQueryClient();
  const updateProfile = useMutation({
    mutationFn: api.updateProfile,
    onSuccess: (data) => queryClient.setQueryData(["configuration"], data)
  });

  if (!configuration) {
    return <Panel title={t("configuration.title")}>{t("common.loadingConfiguration")}</Panel>;
  }

  return (
    <section className="stack">
      <div className="page-grid">
        <MetricCard title={t("configuration.aggressiveness")} value={configuration.aggressiveness.level} helper={configuration.aggressiveness.description} />
        <MetricCard title={t("configuration.kafkaInput")} value={configuration.kafka.inputTopic} helper={configuration.kafka.bootstrapServers} />
        <MetricCard title={t("configuration.kafkaOutput")} value={configuration.kafka.outputTopic} helper={configuration.kafka.applicationId} />
        <MetricCard title={t("configuration.playback")} value={configuration.kafka.playbackInterval} helper={t("configuration.playbackHelper")} />
      </div>
      <Panel title={t("configuration.algorithms")}>
        <div className="algorithm-list">
          {configuration.registeredAlgorithms.map((algorithm) => (
            <span className="badge" key={algorithm}>{algorithm}</span>
          ))}
        </div>
        <p className="panel-note">
          {t("configuration.algorithmsNote")}
        </p>
      </Panel>
      <Panel title={t("configuration.runtimeProfile")}>
        {updateProfile.isPending && <Notice tone="info" text={t("configuration.profilePending")} />}
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
          {t("configuration.profileHelp")}
        </p>
      </Panel>
      <Panel title={t("configuration.detectors")}>
        <div className="detector-grid">
          {configuration.detectors.map((detector) => (
            <article className="detector-card" key={detector.name}>
              <div className="detector-head">
                <strong>{detector.name}</strong>
                <span className="badge">{detector.sensitivity}</span>
              </div>
              <dl>
                <dt>{t("configuration.algorithm")}</dt>
                <dd>{detector.algorithm}</dd>
                <dt>{t("configuration.metrics")}</dt>
                <dd>{detector.metrics.join(", ") || t("common.any")}</dd>
                <dt>{t("configuration.warning")}</dt>
                <dd>{detector.warningThreshold} / p={detector.warningPValue}</dd>
                <dt>{t("configuration.critical")}</dt>
                <dd>{detector.criticalThreshold} / p={detector.criticalPValue}</dd>
                <dt>{t("configuration.emission")}</dt>
                <dd>
                  {t("configuration.emissionValue", {
                    signals: detector.emissionPolicy.minConsecutiveSignals,
                    cooldown: detector.emissionPolicy.cooldown,
                    recovery: detector.emissionPolicy.recoveryConsecutiveNormal
                  })}
                </dd>
              </dl>
            </article>
          ))}
        </div>
      </Panel>
    </section>
  );
}
