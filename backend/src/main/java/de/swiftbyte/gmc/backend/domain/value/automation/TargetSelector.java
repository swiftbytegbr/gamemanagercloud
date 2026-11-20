package de.swiftbyte.gmc.backend.domain.value.automation;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

/**
 * Base type for target selection on an automation.
 * Concrete implementations:
 *  - ExplicitTargetSelector: fixed list of ids
 *  - DynamicTargetSelector: ANDed list of key/op/value filters
 *  - TriggerSourceTargetSelector: single id from the triggering state-change event
 * Manual selection is represented by a null reference.
 */
@Data
@TypeAlias("auto.targets")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExplicitTargetSelector.class, name = "EXPLICIT"),
        @JsonSubTypes.Type(value = DynamicTargetSelector.class, name = "DYNAMIC"),
        @JsonSubTypes.Type(value = TriggerSourceTargetSelector.class, name = "TRIGGER_SOURCE")
})
public abstract class TargetSelector { }
