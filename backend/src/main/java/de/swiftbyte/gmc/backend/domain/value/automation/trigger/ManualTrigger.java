package de.swiftbyte.gmc.backend.domain.value.automation.trigger;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.trigger.manual")
public class ManualTrigger extends AutomationTrigger {
    public ManualTrigger() {
        setType(AutomationTriggerType.MANUAL);
    }
}
