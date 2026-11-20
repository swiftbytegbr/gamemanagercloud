package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.TimedWaitStep;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TimedWaitStepExecutor implements AutomationStepExecutor<TimedWaitStep> {
    @Override
    public AutomationStepType getType() { return AutomationStepType.TIMED_WAIT; }

    @Override
    public void execute(Automation automation, AutomationRun run, TimedWaitStep step) {
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null) return;

        run.setStatus(AutomationRunStatus.PENDING);
        int minutes = step.getWaitMinutes() != null ? step.getWaitMinutes() : 0;
        Instant wake = Instant.now().plus(Duration.ofMinutes(minutes));
        state.setWakeAt(wake);
        state.setWaitingFor(null);
    }
}
