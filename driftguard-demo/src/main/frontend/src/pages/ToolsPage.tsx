import { Boxes, ExternalLink, Route } from "lucide-react";
import { Panel } from "../components/ui";
import { useI18n } from "../i18n";
import type { DemoHelp, ToolLink } from "../types";

export function ToolsPage({ endpoints, tools }: { endpoints: DemoHelp; tools: ToolLink[] }) {
  const { t } = useI18n();
  const endpointEntries = Object.entries(endpoints).sort(([left], [right]) => left.localeCompare(right));
  return (
    <section className="stack">
      <div className="tool-grid">
        {tools.map((tool) => (
          <a className="tool-card" href={tool.url} key={tool.id} rel="noreferrer" target={tool.url.startsWith("http") ? "_blank" : undefined}>
            <span className="tool-icon"><Boxes size={22} /></span>
            <strong>{tool.title}</strong>
            <span>{tool.description}</span>
            <small><ExternalLink size={13} />{tool.url}</small>
          </a>
        ))}
      </div>
      <Panel title={t("tools.apiSurface")}>
        {endpointEntries.length === 0 ? (
          <div className="empty-state compact">{t("tools.loading")}</div>
        ) : (
          <div className="endpoint-grid">
            {endpointEntries.map(([name, endpoint]) => (
              <article className="endpoint-card" key={name}>
                <Route size={16} />
                <div>
                  <strong>{name}</strong>
                  <code>{endpoint}</code>
                </div>
              </article>
            ))}
          </div>
        )}
      </Panel>
    </section>
  );
}
