import { Panel, Progress } from "../../components/ui";
import type { KafkaDemoStatus } from "../../types";

export function ProducerStrip({ status }: { status?: KafkaDemoStatus }) {
  const producers = status?.producers ?? [];

  if (producers.length === 0) {
    return null;
  }

  return (
    <Panel title="Producers" className="quiet-panel">
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
        Панель держит операционный контекст рядом с графиками: состояние topology, producer-ов, прогресс replay и последний drift event.
      </p>
    </Panel>
  );
}
