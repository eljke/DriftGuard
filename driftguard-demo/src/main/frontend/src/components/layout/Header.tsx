import { StatusPill } from "../ui";
import type { DemoRunResult, KafkaDemoStatus } from "../../types";
import { useI18n } from "../../i18n";

export function Header({ overview, kafka }: { overview?: DemoRunResult; kafka?: KafkaDemoStatus }) {
  const { locale, setLocale, t } = useI18n();

  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">{t("app.eyebrow")}</p>
        <h1>{t("app.title")}</h1>
      </div>
      <div className="status-strip">
        <div className="language-switcher" aria-label={t("language.label")}>
          <button className={locale === "ru" ? "active" : ""} onClick={() => setLocale("ru")} type="button">RU</button>
          <button className={locale === "en" ? "active" : ""} onClick={() => setLocale("en")} type="button">EN</button>
        </div>
        <StatusPill label={t("status.synthetic")} active={Boolean(overview?.running)} />
        <StatusPill label={t("status.kafka")} active={Boolean(kafka?.running)} />
      </div>
    </header>
  );
}
