package ru.eljke.driftguard.core.config;

/**
 * Calendar regimes used to isolate detector baselines.
 */
public enum CalendarBaselineMode {
    /**
     * Use one detector state for the metric stream.
     */
    DISABLED,

    /**
     * Keep separate baselines for every hour in a day.
     */
    HOUR_OF_DAY,

    /**
     * Keep separate baselines for every weekday/hour pair.
     */
    HOUR_OF_WEEK
}
