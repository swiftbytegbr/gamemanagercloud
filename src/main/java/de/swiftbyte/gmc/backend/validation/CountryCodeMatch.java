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
@Constraint(validatedBy = CountryCodeMatchValidator.class)
public @interface CountryCodeMatch {
    String message() default "company_country_mismatch";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // Dot-separated property paths from the annotated object.
    String left();

    String right();
}

