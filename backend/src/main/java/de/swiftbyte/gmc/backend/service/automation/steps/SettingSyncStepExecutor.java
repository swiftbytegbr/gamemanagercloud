package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.SettingSyncStep;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.dto.CopyProfileRequest;
import de.swiftbyte.gmc.backend.service.ServerService;
import de.swiftbyte.gmc.backend.service.setting.SettingProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingSyncStepExecutor implements AutomationStepExecutor<SettingSyncStep> {
    private final ServerService serverService;
    private final SettingProfileService settingProfileService;
    @Override public AutomationStepType getType() { return AutomationStepType.SETTING_SYNC; }
    @Override public void execute(Automation automation, AutomationRun run, SettingSyncStep step) {
        var state = run.getCurrentStep(); if (state == null) return;
        for (Map.Entry<String, Boolean> e : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) continue;
            String targetId = e.getKey();
            try {
                if (step.getProfileId() == null || step.getProfileId().isBlank()) {
                    state.putStatus(targetId, AutomationStepStatus.FAILED, "profile_id_missing", Instant.now());
                    continue;
                }
                var server = serverService.getOrThrow(targetId);
                CopyProfileRequest req = new CopyProfileRequest();
                req.setSourceId(step.getProfileId());
                req.setDestinationIds(java.util.List.of(server.getSettingProfileId()));
                req.setGameSettingKeys(step.getGameSettingKeys());
                req.setGameUserSettingKeys(step.getGameUserSettingKeys());
                req.setHyphenParamKeys(step.getHyphenParamKeys());
                req.setQuestionMarkParamKeys(step.getQuestionMarkParamKeys());
                req.setGmcSettingKeys(step.getGmcSettingKeys());
                settingProfileService.syncProfileToServer(step.getProfileId(), server, req);
                state.putStatus(targetId, AutomationStepStatus.SUCCEEDED, null, Instant.now());
            } catch (Exception ex) {
                state.putStatus(targetId, AutomationStepStatus.FAILED, ex.getMessage(), Instant.now());
            }
        }
    }
}
