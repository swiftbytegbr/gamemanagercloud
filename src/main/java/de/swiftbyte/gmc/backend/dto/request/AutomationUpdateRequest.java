package de.swiftbyte.gmc.backend.dto.request;

import de.swiftbyte.gmc.backend.domain.value.automation.TargetSelector;
import de.swiftbyte.gmc.backend.domain.value.automation.trigger.AutomationTrigger;
import de.swiftbyte.gmc.backend.domain.value.automation.workflow.AutomationStep;
import de.swiftbyte.gmc.backend.util.AutomationConstraints;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AutomationUpdateRequest {
    @NotBlank
    private String name;

    private String description;

    private boolean enabled;

    @Valid
    private TargetSelector targets;

    @Size(max = AutomationConstraints.MAX_WORKFLOW_STEPS)
    @Valid
    private List<AutomationStep> workflow;

    @NotNull
    @Valid
    private AutomationTrigger trigger;

    @Max(AutomationConstraints.MAX_TIMEOUT_SECONDS)
    private Integer runTimeoutSec;
}

