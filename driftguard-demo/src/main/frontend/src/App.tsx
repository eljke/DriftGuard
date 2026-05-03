import { useQuery } from "@tanstack/react-query";
import { Activity, BarChart3, Cable, Gauge, Settings, Wrench } from "lucide-react";
import { useState } from "react";
import { api } from "./api/client";
import { ApiStatusBanner, StatusPill } from "./components/ui";
import { KafkaPage } from "./features/kafka/KafkaPage";
import { ConfigurationPage } from "./pages/ConfigurationPage";
import { OverviewPage } from "./pages/OverviewPage";
import { SyntheticPage } from "./pages/SyntheticPage";
import { ToolsPage } from "./pages/ToolsPage";
import type { DemoRunResult, KafkaDemoStatus } from "./types";

type Page = "overview" | "synthetic" | "kafka" | "configuration" | "tools";

const navigation: Array<{ page: Page; label: string; icon: typeof Activity }> = [
  { page: "overview", label: "Overview", icon: Activity },
  { page: "synthetic", label: "Synthetic", icon: BarChart3 },
  { page: "kafka", label: "Kafka Demo", icon: Cable },
  { page: "configuration", label: "Configuration", icon: Settings },
  { page: "tools", label: "Tools", icon: Wrench }
];

export default function App() {
  const [page, setPage] = useState<Page>("overview");
  const overview = useQuery({ queryKey: ["overview"], queryFn: api.overview, refetchInterval: 750 });
  const scenarios = useQuery({ queryKey: ["scenarios"], queryFn: api.scenarios });
  const kafka = useQuery({ queryKey: ["kafka"], queryFn: api.kafkaStatus, refetchInterval: 750 });
  const kafkaOperations = useQuery({ queryKey: ["kafka-operations"], queryFn: api.kafkaOperations, refetchInterval: 1500 });
  const tools = useQuery({ queryKey: ["tools"], queryFn: api.tools });
  const configuration = useQuery({ queryKey: ["configuration"], queryFn: api.configuration });
  const storedEvents = useQuery({ queryKey: ["stored-events"], queryFn: api.storedEvents, refetchInterval: 750 });

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">
            <Gauge size={24} />
          </div>
          <div>
            <strong>DriftGuard</strong>
            <span>stream drift detection</span>
          </div>
        </div>
        <nav className="nav">
          {navigation.map((item) => {
            const Icon = item.icon;
            return (
              <button
                className={page === item.page ? "nav-item active" : "nav-item"}
                key={item.page}
                onClick={() => setPage(item.page)}
                type="button"
              >
                <Icon size={18} />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <main className="main">
        <Header overview={overview.data} kafka={kafka.data} />
        <ApiStatusBanner
          items={[
            { label: "Overview", error: overview.error, retry: () => overview.refetch() },
            { label: "Scenarios", error: scenarios.error, retry: () => scenarios.refetch() },
            { label: "Kafka status", error: kafka.error, retry: () => kafka.refetch() },
            { label: "Kafka operations", error: kafkaOperations.error, retry: () => kafkaOperations.refetch() },
            { label: "Configuration", error: configuration.error, retry: () => configuration.refetch() },
            { label: "Stored events", error: storedEvents.error, retry: () => storedEvents.refetch() }
          ]}
        />
        {page === "overview" && <OverviewPage result={overview.data} kafka={kafka.data} storedEvents={storedEvents.data ?? []} />}
        {page === "synthetic" && <SyntheticPage result={overview.data} scenarios={scenarios.data ?? []} />}
        {page === "kafka" && <KafkaPage status={kafka.data} operations={kafkaOperations.data} scenarios={scenarios.data ?? []} configuration={configuration.data} />}
        {page === "configuration" && <ConfigurationPage configuration={configuration.data} />}
        {page === "tools" && <ToolsPage tools={tools.data ?? []} />}
      </main>
    </div>
  );
}

function Header({ overview, kafka }: { overview?: DemoRunResult; kafka?: KafkaDemoStatus }) {
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
