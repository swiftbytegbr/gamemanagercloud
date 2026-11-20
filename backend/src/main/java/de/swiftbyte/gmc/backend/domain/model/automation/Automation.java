package de.swiftbyte.gmc.backend.domain.model.automation;

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
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Document("automations")
@CompoundIndexes({
        // Scheduled trigger scanner: enabled + trigger.type + trigger.nextFireAt + trigger.leaseUntil
        @CompoundIndex(name = "auto_sched_claim",
                def = "{ 'enabled':1, 'trigger.type':1, 'trigger.nextFireAt':1, 'trigger.leaseUntil':1 }"),
        // State-change trigger matcher: enabled + teamId + trigger.type + trigger.newState + trigger.serverIds (multikey)
        @CompoundIndex(name = "auto_state_trigger",
                def = "{ 'enabled':1, 'teamId':1, 'trigger.type':1, 'trigger.newState':1, 'trigger.serverIds':1 }"),
        // Generic team listing by update time
        @CompoundIndex(name = "auto_team_updated", def = "{ 'teamId':1, 'updatedAt':-1 }")
})
public class Automation {

    @Id
    private String id;

    @Indexed
    private String teamId;

    @NotBlank(message = "automation.name_required")
    private String name;

    private String description;

    @Indexed
    private boolean enabled;

    private Instant createdAt;

    private Instant updatedAt;

    // Polymorphic selector (explicit or dynamic). Null implies runtime/manual selection.
    private TargetSelector targets;


    // Workflow definition: ordered list of steps
    @Size(max = AutomationConstraints.MAX_WORKFLOW_STEPS, message = "automation.workflow_too_large")
    @Valid
    private List<AutomationStep> workflow = new ArrayList<>();

    // Trigger for this automation (manual/scheduled/stateChange)
    @NotNull(message = "automation.trigger_required")
    @Valid
    private AutomationTrigger trigger;

    // Optional hard cap for a run (seconds)
    @Max(value = AutomationConstraints.MAX_TIMEOUT_SECONDS, message = "automation.run_timeout_too_long")
    private Integer runTimeoutSec;
}
