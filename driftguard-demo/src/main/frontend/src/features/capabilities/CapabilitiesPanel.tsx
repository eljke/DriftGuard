import { useMemo } from "react";
import { Panel } from "../../components/ui";
import type { DemoCapability, DemoCapabilityGroup, DemoCapabilityStatus } from "../../types";

const statusLabels: Record<DemoCapabilityStatus, string> = {
    READY: "Ready",
    PARTIAL: "Partial",
    PLANNED: "Planned"
};

export function CapabilitiesPanel({ groups }: { groups: DemoCapabilityGroup[] }) {
    const totals = useMemo(() => summarize(groups), [groups]);

    if (groups.length === 0) {
        return (
            <Panel className="wide capabilities-panel" title="Demo capability map">
                <div className="empty-state compact">Карта возможностей загружается.</div>
            </Panel>
        );
    }

    return (
        <Panel className="wide capabilities-panel" title="Demo capability map">
            <div className="capability-summary">
                <div>
                    <span>Ready</span>
                    <strong>{totals.READY}</strong>
                </div>
                <div>
                    <span>Partial</span>
                    <strong>{totals.PARTIAL}</strong>
                </div>
                <div>
                    <span>Planned</span>
                    <strong>{totals.PLANNED}</strong>
                </div>
            </div>

            <div className="capability-groups">
                {groups.map((group) => (
                    <section className="capability-group" key={group.id}>
                        <div className="capability-group-head">
                            <div>
                                <h3>{group.title}</h3>
                                <p>{group.description}</p>
                            </div>
                            <span className="badge">{group.capabilities.length} items</span>
                        </div>

                        <div className="capability-list">
                            {group.capabilities.map((capability) => (
                                <CapabilityCard capability={capability} key={capability.id} />
                            ))}
                        </div>
                    </section>
                ))}
            </div>
        </Panel>
    );
}

function CapabilityCard({ capability }: { capability: DemoCapability }) {
    return (
        <article className="capability-card">
            <div className="capability-card-head">
                <div>
                    <strong>{capability.title}</strong>
                    <span>{capability.description}</span>
                </div>
                <span className={`capability-status ${capability.status.toLowerCase()}`}>
          {statusLabels[capability.status]}
        </span>
            </div>

            <dl className="capability-meta">
                <div>
                    <dt>Category</dt>
                    <dd>{capability.category}</dd>
                </div>
                <div>
                    <dt>UI</dt>
                    <dd>{capability.uiSurfaces.join(", ") || "—"}</dd>
                </div>
            </dl>

            <div className="capability-endpoints">
                {capability.apiEndpoints.map((endpoint) => (
                    <code key={endpoint}>{endpoint}</code>
                ))}
            </div>
        </article>
    );
}

function summarize(groups: DemoCapabilityGroup[]): Record<DemoCapabilityStatus, number> {
    const totals: Record<DemoCapabilityStatus, number> = {
        READY: 0,
        PARTIAL: 0,
        PLANNED: 0
    };

    for (const group of groups) {
        for (const capability of group.capabilities) {
            totals[capability.status] += 1;
        }
    }

    return totals;
}