package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.wipe_savegames")
public class WipeSavegamesStep extends AutomationStep {
    public WipeSavegamesStep() { setType(AutomationStepType.WIPE_SAVEGAMES); }
}

