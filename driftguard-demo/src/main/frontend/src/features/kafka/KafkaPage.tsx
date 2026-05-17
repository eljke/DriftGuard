import { Panel } from "../../components/ui";
import { EventsTable } from "../events/EventsTable";
import { IncidentsPanel } from "../events/IncidentsPanel";
import { StreamGrid } from "../common/StreamGrid";
import type { DemoConfigurationView, DemoScenarioDescriptor, KafkaDemoStatus, KafkaOperationsSnapshot } from "../../types";
import { KafkaOperationsPanel } from "./KafkaOperationsPanel";
import { KafkaScenarioPanel } from "./KafkaScenarioPanel";
import { ProducerStrip } from "./ProducerStrip";
import { useKafkaDemo } from "./useKafkaDemo";
import { useI18n } from "../../i18n";

export function KafkaPage({
  status,
  operations,
  scenarios,
  configuration
}: {
  status?: KafkaDemoStatus;
  operations?: KafkaOperationsSnapshot;
  scenarios: DemoScenarioDescriptor[];
  configuration?: DemoConfigurationView;
}) {
  const { t } = useI18n();
  const kafkaDemo = useKafkaDemo();
  const kafkaScenarios = scenarios.filter((scenario) => scenario.id !== "seasonal-latency");
  const profiles = configuration?.availableProfiles ?? [];

  return (
    <section className="stack kafka-page">
      <KafkaOperationsPanel status={status} operations={operations} />
      <KafkaScenarioPanel
        busy={kafkaDemo.busy}
        error={kafkaDemo.error}
        profiles={profiles}
        replayProfile={kafkaDemo.replayProfile}
        replaySpeed={kafkaDemo.replaySpeed}
        resetState={kafkaDemo.resetState}
        scenarioParams={kafkaDemo.scenarioParams}
        scenarios={kafkaScenarios}
        status={status}
        stopping={kafkaDemo.stopping}
        onProfileChange={kafkaDemo.setReplayProfile}
        onReplay={kafkaDemo.replayScenario}
        onResetStateChange={kafkaDemo.setResetState}
        onScenarioParamsChange={kafkaDemo.setScenarioParams}
        onRun={kafkaDemo.startScenario}
        onSpeedChange={kafkaDemo.setReplaySpeed}
        onStop={() => kafkaDemo.stop()}
      />
      <ProducerStrip status={status} />
      <Panel title={t("kafka.metricStreams")}>
        <StreamGrid points={status?.samplePoints ?? []} events={status?.consumedEvents ?? []} running={Boolean(status?.running)} />
      </Panel>
      <IncidentsPanel events={status?.consumedEvents ?? []} />
      <EventsTable events={status?.consumedEvents ?? []} />
    </section>
  );
}
