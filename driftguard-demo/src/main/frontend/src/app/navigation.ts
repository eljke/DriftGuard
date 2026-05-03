import { Activity, BarChart3, Cable, Settings, Wrench } from "lucide-react";

export type Page = "overview" | "synthetic" | "kafka" | "configuration" | "tools";

export const navigation: Array<{ page: Page; label: string; icon: typeof Activity }> = [
  { page: "overview", label: "Overview", icon: Activity },
  { page: "synthetic", label: "Synthetic", icon: BarChart3 },
  { page: "kafka", label: "Kafka Demo", icon: Cable },
  { page: "configuration", label: "Configuration", icon: Settings },
  { page: "tools", label: "Tools", icon: Wrench }
];
