import { Gauge } from "lucide-react";
import { navigation, type Page } from "../../app/navigation";
import { useI18n } from "../../i18n";

export function Sidebar({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  const { t } = useI18n();

  return (
    <aside className="sidebar">
      <div className="brand">
        <div className="brand-mark">
          <Gauge size={24} />
        </div>
        <div>
          <strong>DriftGuard</strong>
          <span>{t("app.subtitle")}</span>
        </div>
      </div>
      <nav className="nav">
        {navigation.map((item) => {
          const Icon = item.icon;
          return (
            <button
              className={page === item.page ? "nav-item active" : "nav-item"}
              key={item.page}
              onClick={() => onPageChange(item.page)}
              type="button"
            >
              <Icon size={18} />
              {t(item.labelKey)}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
