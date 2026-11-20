package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.notify")
public class NotifyStep extends AutomationStep {

    // Optional title for the team notification
    @Size(max = 200, message = "automation.notify_title_too_long")
    private String title;

    // Notification body message
    @Size(max = 4000, message = "automation.notify_message_too_long")
    private String message;

    // Severity to show to team members (INFO, WARNING, ERROR, CRITICAL)
    private NotificationSeverity severity;

    public NotifyStep() {
        setType(AutomationStepType.NOTIFY);
    }
}
