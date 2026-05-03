import { Activity, AlertCircle } from "lucide-react";

export function Notice({ tone, text }: { tone: "info" | "error"; text: string }) {
  return (
    <div className={`notice ${tone}`}>
      {tone === "error" ? <AlertCircle size={16} /> : <Activity size={16} />}
      <span>{text}</span>
    </div>
  );
}
