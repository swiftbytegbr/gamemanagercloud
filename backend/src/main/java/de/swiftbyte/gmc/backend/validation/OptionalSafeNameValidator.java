package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class OptionalSafeNameValidator implements ConstraintValidator<OptionalSafeName, String> {
    private int maxLen;

    @Override
    public void initialize(OptionalSafeName constraintAnnotation) {
        this.maxLen = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Optional: null/blank means not provided -> valid
            return true;
        }
        if (value.length() > maxLen) return false;
        // Allow unicode letters/numbers, space, underscore, dot, dash
        return value.matches("^[\\p{L}\\p{N} _.-]+$");
    }
}

