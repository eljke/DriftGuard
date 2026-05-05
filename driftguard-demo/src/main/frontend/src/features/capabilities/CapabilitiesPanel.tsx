import { CheckCircle2, CircleDashed, TriangleAlert } from "lucide-react";
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
                    <h3>Backend coverage surfaced in UI</h3>
                    <p>Каждая возможность связывает user-facing экран с REST endpoints, чтобы demo показывала реальные сценарии starter-а, Kafka module, testkit и engine.</p>
                </div>
                <div className="capability-totals">
                    <CapabilityTotal label="Ready" value={totals.READY} tone="ready" />
                    <CapabilityTotal label="Partial" value={totals.PARTIAL} tone="partial" />
                    <CapabilityTotal label="Planned" value={totals.PLANNED} tone="planned" />
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
    const Icon = capability.status === "READY" ? CheckCircle2 : capability.status === "PARTIAL" ? TriangleAlert : CircleDashed;
    return (
        <article className={`capability-card ${capability.status.toLowerCase()}`}>
            <div className="capability-card-head">
                <div>
                    <strong>{capability.title}</strong>
                </div>
                <span className={`capability-status ${capability.status.toLowerCase()}`}>
                    <Icon size={14} />
                    {statusLabels[capability.status]}
                </span>
            </div>
            <p>{capability.description}</p>

            <div className="capability-chips">
                <span>UI</span>
                <div>
                    {capability.uiSurfaces.map((surface) => (
                        <span className="capability-chip" key={surface}>{surface}</span>
                    ))}
                </div>
            </div>
            <div className="capability-chips">
                <span>API</span>
                <div>
                    {capability.apiEndpoints.map((endpoint) => (
                        <code className="capability-chip mono" key={endpoint}>{endpoint}</code>
                    ))}
                </div>
            </div>
        </article>
    );
}

function CapabilityTotal({ label, value, tone }: { label: string; value: number; tone: "ready" | "partial" | "planned" }) {
    return (
        <div className={`capability-total ${tone}`}>
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
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
