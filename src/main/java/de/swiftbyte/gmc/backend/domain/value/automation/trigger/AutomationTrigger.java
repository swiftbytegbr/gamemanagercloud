package de.swiftbyte.gmc.backend.domain.value.automation.trigger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationTriggerType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Base type for automation triggers. Stored as polymorphic sub-documents in MongoDB.
 */
@Data
@TypeAlias("auto.trigger")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ManualTrigger.class, name = "MANUAL"),
        @JsonSubTypes.Type(value = ScheduledTrigger.class, name = "SCHEDULED"),
        @JsonSubTypes.Type(value = StateChangeTrigger.class, name = "STATE_CHANGE")
})
@NoArgsConstructor
@AllArgsConstructor
@RequestMapping
public abstract class AutomationTrigger {
    // No trigger-specific id; identify by array index within the Automation.
    private AutomationTriggerType type;
}
