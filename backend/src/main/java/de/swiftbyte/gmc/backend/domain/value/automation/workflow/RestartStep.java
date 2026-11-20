package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.restart")
public class RestartStep extends AutomationStep {
    private String message;
    private Integer delayMinutes;
    public RestartStep() { setType(AutomationStepType.RESTART); }
}
