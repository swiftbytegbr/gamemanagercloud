package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@NotBlank(message = "invalid_id")
@Size(min = 1, max = 128, message = "invalid_id")
@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "invalid_id")
@Constraint(validatedBy = {})
public @interface LongIdentifier {
    String message() default "invalid_id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

