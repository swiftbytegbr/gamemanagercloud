package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class CountryCodeMatchValidator implements ConstraintValidator<CountryCodeMatch, Object> {
    private String leftPath;
    private String rightPath;

    @Override
    public void initialize(CountryCodeMatch constraintAnnotation) {
        this.leftPath = constraintAnnotation.left();
        this.rightPath = constraintAnnotation.right();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        String left = readPath(value, leftPath);
        String right = readPath(value, rightPath);

        // Only enforce if both sides are present
        if (isBlank(left) || isBlank(right)) {
            return true;
        }

        // Compare case-insensitively after trimming
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String readPath(Object root, String path) {
        try {
            Object current = root;
            for (String part : path.split("\\.")) {
                if (current == null) return null;
                current = invokeGetter(current, part);
            }
            return current == null ? null : String.valueOf(current);
        } catch (Exception e) {
            return null;
        }
    }

    private Object invokeGetter(Object obj, String property) {
        try {
            String base = property.substring(0, 1).toUpperCase() + property.substring(1);
            Method getter = null;
            try {
                getter = obj.getClass().getMethod("get" + base);
            } catch (NoSuchMethodException ignored) {
                // try boolean-style getter
                try {
                    getter = obj.getClass().getMethod("is" + base);
                } catch (NoSuchMethodException ex) {
                    return null;
                }
            }
            return getter.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}

