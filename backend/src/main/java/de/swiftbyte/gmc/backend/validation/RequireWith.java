package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = RequireWithValidator.class)
public @interface RequireWith {
    String message() default "address_required";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String left();  // if this is present (non-null), then right must be present

    String right(); // this must be present if left is
}

