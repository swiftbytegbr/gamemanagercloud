package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ServerDirectoryPathStringValidator implements ConstraintValidator<ServerDirectoryPath, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // null is allowed
        if (value.isBlank()) return false; // blank is not allowed
        return ServerDirectoryPathValidatorUtil.isValidPath(value);
    }
}
