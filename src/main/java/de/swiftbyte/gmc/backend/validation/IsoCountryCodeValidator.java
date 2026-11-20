package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class IsoCountryCodeValidator implements ConstraintValidator<IsoCountryCode, String> {

    private static final Set<String> ISO_ALPHA2 = new HashSet<>(Arrays.asList(Locale.getISOCountries()));

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            // Let @NotBlank or other annotations handle presence
            return true;
        }
        // Must be exactly two uppercase letters and a valid ISO code
        if (value.length() != 2) return false;
        if (!value.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z')) return false;
        return ISO_ALPHA2.contains(value);
    }
}

