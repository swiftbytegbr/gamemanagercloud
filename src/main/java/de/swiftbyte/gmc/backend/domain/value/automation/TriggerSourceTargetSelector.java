package de.swiftbyte.gmc.backend.domain.value.automation;

import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

/**
 * Selects the single target that triggered the automation (server id from the state change event).
 * Valid only when the automation trigger type is STATE_CHANGE.
 */
@Data
@TypeAlias("auto.targets.trigger_source")
public class TriggerSourceTargetSelector extends TargetSelector { }

