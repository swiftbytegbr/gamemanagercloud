package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.AutomationStep;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AutomationStepExecutorRegistry {

    private final Map<AutomationStepType, AutomationStepExecutor<?>> byType = new HashMap<>();

    public AutomationStepExecutorRegistry(List<AutomationStepExecutor<?>> executors) {
        if (executors != null) {
            for (AutomationStepExecutor<?> e : executors) {
                byType.put(e.getType(), e);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void execute(Automation automation, AutomationRun run, AutomationStep step) {
        if (step == null || step.getType() == null) return;
        AutomationStepExecutor exec = byType.get(step.getType());
        if (exec == null) return;
        exec.execute(automation, run, step);
    }
}

