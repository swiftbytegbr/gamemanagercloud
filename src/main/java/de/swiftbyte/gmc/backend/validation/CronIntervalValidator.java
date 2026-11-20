package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.scheduling.support.CronExpression;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class CronIntervalValidator implements ConstraintValidator<CronInterval, String> {

    private int minMinutes;

    @Override
    public void initialize(CronInterval constraintAnnotation) {
        this.minMinutes = constraintAnnotation.minMinutes();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/blank here; combine with @NotBlank on the field when required
        if (value == null || value.isBlank()) return true;

        CronExpression cron;
        try {
            cron = CronExpression.parse(value);
        } catch (Exception e) {
            // Override message to specific invalid cron key
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("automation.cron_invalid")
                    .addConstraintViolation();
            return false;
        }

        ZonedDateTime base = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime next1 = cron.next(base);
        if (next1 == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("automation.cron_invalid")
                    .addConstraintViolation();
            return false;
        }
        ZonedDateTime next2 = cron.next(next1);
        if (next2 == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("automation.cron_invalid")
                    .addConstraintViolation();
            return false;
        }

        long minutes = Duration.between(next1, next2).toMinutes();
        if (minutes < minMinutes) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("automation.cron_interval_too_short")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}

