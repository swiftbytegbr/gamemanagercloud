package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.AutomationStep;

public interface AutomationStepExecutor<T extends AutomationStep> {
    AutomationStepType getType();
    void execute(Automation automation, AutomationRun run, T step);
}

