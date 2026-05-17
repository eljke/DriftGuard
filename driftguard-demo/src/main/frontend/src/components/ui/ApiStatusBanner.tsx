import { AlertCircle, RefreshCw } from "lucide-react";
import { useI18n } from "../../i18n";
import { readableError } from "../../lib/format";

interface ApiStatusItem {
  label: string;
  error: unknown;
  retry?: () => void;
}

interface ApiStatusBannerProps {
  items: ApiStatusItem[];
}

export function ApiStatusBanner({ items }: ApiStatusBannerProps) {
  const { t } = useI18n();
  const failed = items.filter((item) => Boolean(item.error));

  if (failed.length === 0) {
    return null;
  }

  return (
    <div className="api-status-banner" role="status">
      <div className="api-status-head">
        <AlertCircle size={18} />
        <div>
          <strong>{t("api.failed.title")}</strong>
          <span>{t("api.failed.subtitle")}</span>
        </div>
      </div>
      <div className="api-status-list">
        {failed.map((item) => (
          <div className="api-status-item" key={item.label}>
            <span>{item.label}</span>
            <small>{readableError(item.error)}</small>
            {item.retry && (
              <button className="ghost-button" onClick={item.retry} type="button">
                <RefreshCw size={14} />
                {t("api.retry")}
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
