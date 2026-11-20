package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.BackupStep;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackupStepExecutor implements AutomationStepExecutor<BackupStep> {
    private final ServerService serverService;
    @Override public AutomationStepType getType() { return AutomationStepType.BACKUP; }
    @Override public void execute(Automation automation, AutomationRun run, BackupStep step) {
        var state = run.getCurrentStep(); if (state == null) return;
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) continue;
            String targetId = e.getKey();
            try {
                if (step.getBackupName() == null || step.getBackupName().isBlank()) {
                    state.putStatus(targetId, AutomationStepStatus.FAILED, "backup_name_missing", Instant.now());
                    continue;
                }
                var server = serverService.getOrThrow(targetId);
                serverService.backupServer(server, step.getBackupName());
                state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
            } catch (Exception ex) {
                state.putStatus(targetId, AutomationStepStatus.FAILED, ex.getMessage(), Instant.now());
            }
        }
    }
}
