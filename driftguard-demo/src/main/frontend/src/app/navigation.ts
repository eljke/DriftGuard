import { Activity, BarChart3, Cable, Settings, Wrench } from "lucide-react";

export type Page = "overview" | "synthetic" | "kafka" | "configuration" | "tools";

export const navigation: Array<{ page: Page; labelKey: string; icon: typeof Activity }> = [
  { page: "overview", labelKey: "nav.overview", icon: Activity },
  { page: "synthetic", labelKey: "nav.synthetic", icon: BarChart3 },
  { page: "kafka", labelKey: "nav.kafka", icon: Cable },
  { page: "configuration", labelKey: "nav.configuration", icon: Settings },
  { page: "tools", labelKey: "nav.tools", icon: Wrench }
];
