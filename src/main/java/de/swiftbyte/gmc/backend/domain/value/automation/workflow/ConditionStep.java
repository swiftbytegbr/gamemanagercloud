package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.condition")
public class ConditionStep extends AutomationStep {

    private GameServerState state;

    public ConditionStep() {
        setType(AutomationStepType.CONDITION);
    }
}
