package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.stop")
public class StopStep extends AutomationStep {
    private String message;
    private Integer delayMinutes;
    private boolean forceStop;
    public StopStep() { setType(AutomationStepType.STOP); }
}
