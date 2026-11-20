package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.enums.automation.AwaitMode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.rcon")
public class RconCommandStep extends AutomationStep {
    private String command;
    // Optional: name of the capture to expose responses under
    // Example usage in templates: {outputs.rcon('<var>').byId('<serverId>').response}
    private String var;

    // Awaiting behavior: AUTO (default), ALWAYS, NEVER
    private AwaitMode awaitMode;

    // Optional per-step timeout (ms) for awaiting RCON responses
    private Integer timeoutMs;
    public RconCommandStep() { setType(AutomationStepType.RCON_COMMAND); }
}
