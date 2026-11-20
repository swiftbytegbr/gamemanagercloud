package de.swiftbyte.gmc.backend.domain.enums.automation;

public enum AutomationStepType {
    START,
    STOP,
    RESTART,
    UPDATE,
    BACKUP,
    WIPE_SAVEGAMES,
    SETTING_SYNC,
    RCON_COMMAND,
    CONDITION,
    // Explicit wait types
    TIMED_WAIT,
    STATE_WAIT,
    NOTIFY,
    DISCORD_WEBHOOK
}
