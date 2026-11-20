package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.RestartStep;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import de.swiftbyte.gmc.common.entity.GameServerState;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RestartStepExecutor implements AutomationStepExecutor<RestartStep> {
    private final ServerService serverService;

    @Override
    public AutomationStepType getType() { return AutomationStepType.RESTART; }

    @Override
    public void execute(Automation automation, AutomationRun run, RestartStep step) {
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null) return;
        // Begin implicit wait for ONLINE on all non-skipped targets
        StateWaitHelper.beginStateWait(run, GameServerState.ONLINE);

        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) continue;
            String targetId = e.getKey();
            try {
                var server = serverService.getOrThrow(targetId);
                serverService.restartServer(server, step.getDelayMinutes(), step.getMessage());
                // Leave status as PENDING; on ONLINE event it will be marked SUCCEEDED
            } catch (Exception ex) {
                state.putStatus(targetId, AutomationStepStatus.FAILED, ex.getMessage(), Instant.now());
            }
        }
    }
}
