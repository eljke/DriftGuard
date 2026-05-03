import { StatusPill } from "../ui";
import type { DemoRunResult, KafkaDemoStatus } from "../../types";

export function Header({ overview, kafka }: { overview?: DemoRunResult; kafka?: KafkaDemoStatus }) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Europe/Moscow · real-time demo</p>
        <h1>Detection cockpit</h1>
      </div>
      <div className="status-strip">
        <StatusPill label="Synthetic" active={Boolean(overview?.running)} />
        <StatusPill label="Kafka" active={Boolean(kafka?.running)} />
      </div>
    </header>
  );
}
