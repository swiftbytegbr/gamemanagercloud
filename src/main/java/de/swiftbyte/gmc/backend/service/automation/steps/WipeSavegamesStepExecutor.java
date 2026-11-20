package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.WipeSavegamesStep;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WipeSavegamesStepExecutor implements AutomationStepExecutor<WipeSavegamesStep> {
    private final ServerService serverService;

    @Override
    public AutomationStepType getType() { return AutomationStepType.WIPE_SAVEGAMES; }

    @Override
    public void execute(Automation automation, AutomationRun run, WipeSavegamesStep step) {
        var state = run.getCurrentStep();
        if (state == null) return;
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) continue;
            String targetId = e.getKey();
            try {
                var server = serverService.getOrThrow(targetId);
                serverService.wipeSavegames(server);
                state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
            } catch (Exception ex) {
                state.putStatus(targetId, AutomationStepStatus.FAILED, ex.getMessage(), Instant.now());
            }
        }
    }
}

