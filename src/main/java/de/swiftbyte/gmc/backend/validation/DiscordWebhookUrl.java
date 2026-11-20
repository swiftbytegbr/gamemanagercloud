package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = DiscordWebhookUrlValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface DiscordWebhookUrl {
    String message() default "automation.invalid_discord_webhook_url";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

