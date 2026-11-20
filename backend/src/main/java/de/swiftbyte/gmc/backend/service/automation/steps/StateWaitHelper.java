package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationRunStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.common.entity.GameServerState;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared helper to initiate a state-based wait in the current step.
 * Used by explicit STATE_WAIT steps and implicit waits after START/STOP/RESTART/UPDATE.
 */
public final class StateWaitHelper {
    private StateWaitHelper() {}

    /**
     * Marks the current step as waiting for the desired state, initializing per-target statuses
     * as PENDING for all non-skipped targets. Sets the run status to PENDING.
     */
    public static void beginStateWait(AutomationRun run, GameServerState desiredState) {
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null) return;
        if (run.getTargetIds() == null || run.getTargetIds().isEmpty()) return;

        if (state.getPerTargetStatus() == null) state.setPerTargetStatus(new HashMap<>());
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                // initialize as PENDING only if not already in a terminal state
                var existing = state.getStateOf(e.getKey());
                if (existing == null || (existing != AutomationStepStatus.SUCCEEDED
                        && existing != AutomationStepStatus.FAILED
                        && existing != AutomationStepStatus.SKIPPED)) {
                    state.putStatus(e.getKey(), AutomationStepStatus.PENDING);
                }
            }
        }
        run.setStatus(AutomationRunStatus.PENDING);
        state.setWaitingFor(desiredState);
    }
}

