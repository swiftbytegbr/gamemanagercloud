package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

/**
 * Explicit timed wait step. Waits for a fixed amount of minutes before resuming.
 * perTargetStatus is not required while waiting (may remain null) and is only
 * populated upon wake when marking targets as SUCCEEDED/FAILED.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.timed_wait")
public class TimedWaitStep extends AutomationStep {
    private Integer waitMinutes;

    public TimedWaitStep() { setType(AutomationStepType.TIMED_WAIT); }
}

