package de.swiftbyte.gmc.backend.domain.value.automation.trigger;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import de.swiftbyte.gmc.common.entity.GameServerState;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.trigger.stateChange")
public class StateChangeTrigger extends AutomationTrigger {

    /**
     * Desired new state to react to (e.g., ONLINE, OFFLINE, STARTING).
     */
    @NotNull(message = "automation.state_trigger_state_required")
    private GameServerState newState;

    /**
     * Explicit server IDs this trigger listens for. If the incoming event's serverId
     * is contained in this list and newState matches, the automation will run.
     */
    @NotEmpty(message = "automation.state_trigger_servers_required")
    private List<String> serverIds = new ArrayList<>();

    public StateChangeTrigger() {
        setType(AutomationTriggerType.STATE_CHANGE);
    }
}
