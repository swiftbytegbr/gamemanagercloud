package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = IsoCountryCodeValidator.class)
public @interface IsoCountryCode {
    String message() default "invalid_country_code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

