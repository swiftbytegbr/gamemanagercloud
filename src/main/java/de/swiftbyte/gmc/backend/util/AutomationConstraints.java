package de.swiftbyte.gmc.backend.util;

/**
 * Centralized constraints and defaults for Automations.
 */
public final class AutomationConstraints {
    private AutomationConstraints() {}

    // Workflow
    public static final int MAX_WORKFLOW_STEPS = 15;

    // Scheduling
    public static final int MIN_CRON_INTERVAL_MINUTES = 15;

    // Timeouts
    public static final int MAX_TIMEOUT_SECONDS = 24 * 60 * 60; // 1 day
    public static final int DEFAULT_TIMEOUT_SECONDS = MAX_TIMEOUT_SECONDS;
}

