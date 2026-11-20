package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.util.AutomationConstraints;
import jakarta.validation.constraints.Max;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

/**
 * Base workflow step; concrete types define behavior.
 */
@Data
@TypeAlias("auto.step")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartStep.class, name = "START"),
        @JsonSubTypes.Type(value = StopStep.class, name = "STOP"),
        @JsonSubTypes.Type(value = RestartStep.class, name = "RESTART"),
        @JsonSubTypes.Type(value = UpdateStep.class, name = "UPDATE"),
        @JsonSubTypes.Type(value = BackupStep.class, name = "BACKUP"),
        @JsonSubTypes.Type(value = WipeSavegamesStep.class, name = "WIPE_SAVEGAMES"),
        @JsonSubTypes.Type(value = SettingSyncStep.class, name = "SETTING_SYNC"),
        @JsonSubTypes.Type(value = RconCommandStep.class, name = "RCON_COMMAND"),
        @JsonSubTypes.Type(value = ConditionStep.class, name = "CONDITION"),
        // Legacy wait type for backward compatibility
        // Explicit wait types
        @JsonSubTypes.Type(value = TimedWaitStep.class, name = "TIMED_WAIT"),
        @JsonSubTypes.Type(value = StateWaitStep.class, name = "STATE_WAIT"),
        @JsonSubTypes.Type(value = NotifyStep.class, name = "NOTIFY"),
        @JsonSubTypes.Type(value = DiscordWebhookStep.class, name = "DISCORD_WEBHOOK")
})
public abstract class AutomationStep {
    private AutomationStepType type;

    /** Optional timeout in seconds for step-level cap. */
    @Max(value = AutomationConstraints.MAX_TIMEOUT_SECONDS, message = "automation.step_timeout_too_long")
    private Integer timeoutSec;
}
