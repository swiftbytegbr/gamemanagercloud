package de.swiftbyte.gmc.backend.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * Generic base for all domain events in the system.
 * Provides common metadata (issuerId and optional timestamp).
 */
@Getter
@RequiredArgsConstructor
public abstract class BaseEvent {
    private final String issuerId; // may be null if no user context
    private final Instant timestamp; // preferred: set at creation
    private boolean actionLog = true; // default enabled
    private String actionLogInformation; // optional human-readable message

    protected BaseEvent(String issuerId) {
        this(issuerId, Instant.now());
    }

    public boolean isActionLogEnabled() {
        return actionLog;
    }

    public void setActionLogEnabled(boolean enabled) { this.actionLog = enabled; }

    public void setActionLogInformation(String info) { this.actionLogInformation = info; }
}
