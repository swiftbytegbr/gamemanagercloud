package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Method;

public class RequireWithValidator implements ConstraintValidator<RequireWith, Object> {
    private String leftPath;
    private String rightPath;
    private String message;

    @Override
    public void initialize(RequireWith constraintAnnotation) {
        this.leftPath = constraintAnnotation.left();
        this.rightPath = constraintAnnotation.right();
        this.message = constraintAnnotation.message();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;

        Object left = readPath(value, leftPath);
        Object right = readPath(value, rightPath);

        if (left != null && right == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    private Object readPath(Object root, String path) {
        try {
            Object current = root;
            for (String part : path.split("\\.")) {
                if (current == null) return null;
                current = invokeGetter(current, part);
            }
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    private Object invokeGetter(Object obj, String property) {
        try {
            String base = property.substring(0, 1).toUpperCase() + property.substring(1);
            Method getter;
            try {
                getter = obj.getClass().getMethod("get" + base);
            } catch (NoSuchMethodException ignored) {
                getter = obj.getClass().getMethod("is" + base);
            }
            return getter.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}

