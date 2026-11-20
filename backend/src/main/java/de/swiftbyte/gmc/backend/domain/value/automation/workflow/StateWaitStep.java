package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

/**
 * Explicit state wait step. Waits until servers reach the desired state.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.state_wait")
public class StateWaitStep extends AutomationStep {
    private GameServerState desiredState;

    public StateWaitStep() { setType(AutomationStepType.STATE_WAIT); }
}

