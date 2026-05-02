package ru.eljke.driftguard.core.state;

/**
 * Version contract for persisted detector runtime state snapshots.
 *
 * <p>The runtime {@link DetectorRuntimeState#version()} is a per-key monotonic
 * revision. This schema version describes the persisted snapshot structure and
 * must be checked by adapters before restoring state from Kafka, JDBC, Redis or
 * any other durable storage.</p>
 */
public final class DetectorRuntimeStateSchema {
    public static final int CURRENT_VERSION = 1;

    private DetectorRuntimeStateSchema() {
    }

    public static boolean isSupported(int schemaVersion) {
        return schemaVersion == CURRENT_VERSION;
    }

    public static void requireSupported(int schemaVersion) {
        if (!isSupported(schemaVersion)) {
            throw new IllegalArgumentException(
                    "Unsupported detector runtime state schema version: "
                            + schemaVersion
                            + ", supported: "
                            + CURRENT_VERSION
            );
        }
    }
}
