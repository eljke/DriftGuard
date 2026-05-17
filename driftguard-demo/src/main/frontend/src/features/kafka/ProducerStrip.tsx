import { Panel, Progress } from "../../components/ui";
import { useI18n } from "../../i18n";
import type { KafkaDemoStatus } from "../../types";

export function ProducerStrip({ status }: { status?: KafkaDemoStatus }) {
  const { t } = useI18n();
  const producers = status?.producers ?? [];

  if (producers.length === 0) {
    return null;
  }

  return (
    <Panel title={t("kafka.producers")} className="quiet-panel">
      <div className="producer-strip">
        {producers.map((producer) => (
          <article className="producer-card compact" key={producer.id}>
            <div>
              <strong>{producer.service}</strong>
              <span>{producer.metric} · {producer.operation ?? "-"}</span>
            </div>
            <Progress value={producer.producedPoints} max={producer.totalPoints} />
          </article>
        ))}
      </div>
      <p className="help-text">
        {t("kafka.producerHelp")}
      </p>
    </Panel>
  );
}
