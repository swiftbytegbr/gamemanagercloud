package de.swiftbyte.gmc.backend.events;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the ActionLogItem.Type associated with an ActionLogEvent.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ActionLogType {
    ActionLogItem.Type value();
}

