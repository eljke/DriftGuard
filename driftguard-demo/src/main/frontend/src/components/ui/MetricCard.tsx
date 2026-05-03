export function MetricCard({ title, value, helper, tone }: { title: string; value: string | number; helper: string; tone?: "danger" }) {
  return (
    <article className={tone === "danger" ? "metric-card danger" : "metric-card"}>
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{helper}</p>
    </article>
  );
}
