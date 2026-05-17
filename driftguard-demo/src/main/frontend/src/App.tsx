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
import { useI18n } from "./i18n";

export default function App() {
  const [page, setPage] = useState<Page>("overview");
  const { t } = useI18n();
  const queries = useDemoQueries();
  const { capabilities, configuration, help, kafka, kafkaOperations, overview, scenarios, storedEvents, tools } = queries;

  return (
    <AppShell page={page} onPageChange={setPage} overview={overview.data} kafka={kafka.data}>
      <ApiStatusBanner
        items={[
          { label: t("nav.overview"), error: overview.error, retry: () => overview.refetch() },
          { label: t("synthetic.title"), error: scenarios.error, retry: () => scenarios.refetch() },
          { label: t("status.kafka"), error: kafka.error, retry: () => kafka.refetch() },
          { label: t("kafka.operations"), error: kafkaOperations.error, retry: () => kafkaOperations.refetch() },
          { label: t("nav.configuration"), error: configuration.error, retry: () => configuration.refetch() },
          { label: t("overview.recentStored"), error: storedEvents.error, retry: () => storedEvents.refetch() },
          { label: t("capabilities.title"), error: capabilities.error, retry: () => capabilities.refetch() },
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
