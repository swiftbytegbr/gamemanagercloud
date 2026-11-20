package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.ConditionStep;
import de.swiftbyte.gmc.backend.util.AutomationRunUtility;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConditionStepExecutor implements AutomationStepExecutor<ConditionStep> {
    private final MongoTemplate mongoTemplate;
    @Override public AutomationStepType getType() { return AutomationStepType.CONDITION; }
    @Override public void execute(Automation automation, AutomationRun run, ConditionStep step) {
        var state = run.getCurrentStep(); if (state == null) return;
        List<String> activeTargets = run.getTargetIds().entrySet().stream().filter(e -> !Boolean.TRUE.equals(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
        Map<String, GameServerState> states = AutomationRunUtility.fetchServerStates(mongoTemplate, activeTargets);
        for (String targetId : run.getTargetIds().keySet()) {
            if (Boolean.TRUE.equals(run.getTargetIds().get(targetId))) continue;
            boolean ok = states.get(targetId) != null && states.get(targetId) == step.getState();
            state.putStatus(targetId, ok ? AutomationStepStatus.SUCCEEDED : AutomationStepStatus.SKIPPED, null, Instant.now());
        }
    }
}

