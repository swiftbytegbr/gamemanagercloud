package de.swiftbyte.gmc.backend.service.automation.steps;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepStatus;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.NotifyStep;
import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.backend.expressions.AutomationExpressionEngine;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import de.swiftbyte.gmc.backend.exception.AutomationException;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyStepExecutor implements AutomationStepExecutor<NotifyStep> {

    private final NotificationService notificationService;
    private final MongoTemplate mongoTemplate;
    private final AutomationExpressionEngine expr;

    @Override
    public AutomationStepType getType() {
        return AutomationStepType.NOTIFY;
    }

    @Override
    public void execute(Automation automation, AutomationRun run, NotifyStep step) {
        AutomationRun.StepState state = run.getCurrentStep();
        if (state == null) return;
        try {
            NotificationSeverity severity = step.getSeverity() != null ? step.getSeverity() : NotificationSeverity.INFO;
            String titleRaw = (step.getTitle() != null && !step.getTitle().isBlank()) ? step.getTitle() : "Automation";
            String messageRaw = step.getMessage() == null ? "" : step.getMessage();

            String title = expr.renderField("notify.title", titleRaw, automation, run);
            String message = expr.renderField("notify.message", messageRaw, automation, run);

            notificationService.create(
                    automation.getTeamId(),
                    "AUTOMATION_CUSTOM",
                    severity,
                    title,
                    message,
                    NotificationSource.BACKEND
            );
        } catch (AutomationException e) {
            String reason = "automation.expression_error";
            log.warn("Expression error during notify rendering: {}", e.getMessage());
            for (Map.Entry<String, Boolean> entry : run.getTargetIds().entrySet()) {
                if (Boolean.TRUE.equals(entry.getValue())) continue;
                state.putStatus(entry.getKey(), AutomationStepStatus.FAILED, reason, Instant.now());
            }
            return;
        } catch (Exception e) {
            log.warn("Failed to create automation notification: {}", e.getMessage());
        }

        for (Map.Entry<String, Boolean> entry : run.getTargetIds().entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) continue;
            state.putStatus(entry.getKey(), AutomationStepStatus.SUCCEEDED, null, Instant.now());
        }
    }
}
