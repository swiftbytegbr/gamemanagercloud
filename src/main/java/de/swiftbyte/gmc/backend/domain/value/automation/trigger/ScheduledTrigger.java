package de.swiftbyte.gmc.backend.domain.value.automation.trigger;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import de.swiftbyte.gmc.backend.validation.CronInterval;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.trigger.scheduled")
public class ScheduledTrigger extends AutomationTrigger {

    /**
     * Cron expression in UTC.
     */
    @NotBlank(message = "automation.cron_required")
    @CronInterval
    private String cron;

    /**
     * Next fire time, used for single-fire claim + advance.
     */
    private Instant nextFireAt;

    /**
     * Optional short lease for claim operations.
     */
    private Instant leaseUntil;

    public ScheduledTrigger() {
        setType(AutomationTriggerType.SCHEDULED);
    }
}
