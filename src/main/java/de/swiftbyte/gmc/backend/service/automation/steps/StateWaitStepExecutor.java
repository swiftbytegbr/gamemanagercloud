package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.StateWaitStep;
import org.springframework.stereotype.Service;

@Service
public class StateWaitStepExecutor implements AutomationStepExecutor<StateWaitStep> {
    @Override
    public AutomationStepType getType() { return AutomationStepType.STATE_WAIT; }

    @Override
    public void execute(Automation automation, AutomationRun run, StateWaitStep step) {
        if (run.getCurrentStep() == null) return;
        if (run.getTargetIds() == null || run.getTargetIds().isEmpty()) return;
        StateWaitHelper.beginStateWait(run, step.getDesiredState());
    }
}
