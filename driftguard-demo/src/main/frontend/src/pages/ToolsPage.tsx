import { Boxes } from "lucide-react";
import type { ToolLink } from "../types";

export function ToolsPage({ tools }: { tools: ToolLink[] }) {
  return (
    <section className="tool-grid">
      {tools.map((tool) => (
        <a className="tool-card" href={tool.url} key={tool.id} rel="noreferrer" target={tool.url.startsWith("http") ? "_blank" : undefined}>
          <Boxes size={22} />
          <strong>{tool.title}</strong>
          <span>{tool.description}</span>
        </a>
      ))}
    </section>
  );
}
