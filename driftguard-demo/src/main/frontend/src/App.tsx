import { useState } from "react";
import { useDemoQueries } from "./app/useDemoQueries";
import type { Page } from "./app/navigation";
import { AppShell } from "./components/layout";
import { ApiStatusBanner } from "./components/ui";
import { KafkaPage } from "./features/kafka/KafkaPage";
import { ConfigurationPage } from "./pages/ConfigurationPage";
import { OverviewPage } from "./pages/OverviewPage";
import { SyntheticPage } from "./pages/SyntheticPage";
import { ToolsPage } from "./pages/ToolsPage";

export default function App() {
  const [page, setPage] = useState<Page>("overview");
  const queries = useDemoQueries();
  const { capabilities, configuration, help, kafka, kafkaOperations, overview, scenarios, storedEvents, tools } = queries;

  return (
    <AppShell page={page} onPageChange={setPage} overview={overview.data} kafka={kafka.data}>
      <ApiStatusBanner
        items={[
          { label: "Overview", error: overview.error, retry: () => overview.refetch() },
          { label: "Scenarios", error: scenarios.error, retry: () => scenarios.refetch() },
          { label: "Kafka status", error: kafka.error, retry: () => kafka.refetch() },
          { label: "Kafka operations", error: kafkaOperations.error, retry: () => kafkaOperations.refetch() },
          { label: "Configuration", error: configuration.error, retry: () => configuration.refetch() },
          { label: "Stored events", error: storedEvents.error, retry: () => storedEvents.refetch() },
          { label: "Capabilities", error: capabilities.error, retry: () => capabilities.refetch() },
          { label: "Help", error: help.error, retry: () => help.refetch() }
        ]}
      />
      {page === "overview" && <OverviewPage result={overview.data} kafka={kafka.data} storedEvents={storedEvents.data ?? []} capabilities={capabilities.data ?? []} />}
      {page === "synthetic" && <SyntheticPage result={overview.data} scenarios={scenarios.data ?? []} />}
      {page === "kafka" && <KafkaPage status={kafka.data} operations={kafkaOperations.data} scenarios={scenarios.data ?? []} configuration={configuration.data} />}
      {page === "configuration" && <ConfigurationPage configuration={configuration.data} />}
      {page === "tools" && <ToolsPage endpoints={help.data ?? {}} tools={tools.data ?? []} />}
    </AppShell>
  );
}
