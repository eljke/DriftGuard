import { Gauge } from "lucide-react";
import { navigation, type Page } from "../../app/navigation";

export function Sidebar({ page, onPageChange }: { page: Page; onPageChange: (page: Page) => void }) {
  return (
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
              onClick={() => onPageChange(item.page)}
              type="button"
            >
              <Icon size={18} />
              {item.label}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
