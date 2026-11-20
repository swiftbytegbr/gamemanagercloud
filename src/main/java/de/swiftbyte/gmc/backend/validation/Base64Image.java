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
@Constraint(validatedBy = Base64ImageValidator.class)
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
public @interface Base64Image {

    // Maximum allowed size in megabytes
    int maxMegabytes() default 5;

    // Default is not used; validator overrides with a human-friendly message
    String message() default "Image exceeds maximum allowed size";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

