package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
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
@NotBlank(message = "invalid_name")
@Size(min = 1, max = 64, message = "invalid_name")
@Pattern(regexp = "^[^\\p{C}\\p{Zl}\\p{Zp}]+$", message = "invalid_name")
@ReportAsSingleViolation
@Constraint(validatedBy = {})
public @interface SafeName {
    String message() default "invalid_name";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
