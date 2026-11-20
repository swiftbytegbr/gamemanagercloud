package de.swiftbyte.gmc.backend.events;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Team;

/**
 * Interface for events that should be persisted to the action log.
 * Provides optional contextual accessors; return null if not applicable.
 */
public interface ActionLogEvent {
    default GameServer getServer() { return null; }
    default Node getNode() { return null; }
    default Team getTeam() { return null; }
    default String getAffectedUserId() { return null; }
    /**
     * Short, human-readable description summarizing the action.
     * Prefer English sentences including key context (e.g. old -> new).
     */
    default String getActionLogInformation() { return this instanceof BaseEvent b ? b.getActionLogInformation() : null; }

    /**
     * Set the human-readable action description. Typically called in the event constructor.
     */
    default void setActionLogInformation(String info) { if (this instanceof BaseEvent b) b.setActionLogInformation(info); }


    /**
     * Whether this event should be written to the action log. Defaults to true.
     */
    default boolean isActionLogEnabled() { return this instanceof BaseEvent b ? b.isActionLogEnabled() : true; }

    /**
     * Disable action logging for this event instance (fluent helper). If the event
     * extends BaseEvent, toggles its internal flag; otherwise no-op.
     */
    @SuppressWarnings("unchecked")
    default <T extends ActionLogEvent> T disableActionLog() {
        if (this instanceof BaseEvent b) b.setActionLogEnabled(false);
        return (T) this;
    }
}
