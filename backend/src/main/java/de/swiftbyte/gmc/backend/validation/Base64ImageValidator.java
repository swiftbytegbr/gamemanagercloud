package de.swiftbyte.gmc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

public class Base64ImageValidator implements ConstraintValidator<Base64Image, String> {

    private int maxMb;
    private long maxBytes;

    @Override
    public void initialize(Base64Image constraintAnnotation) {
        this.maxMb = Math.max(1, constraintAnnotation.maxMegabytes());
        this.maxBytes = (long) this.maxMb * 1024L * 1024L;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Allow null/blank; combine with @NotBlank when required
        if (value == null || value.isBlank()) return true;

        String raw = value.trim();
        // Strip optional data URL prefix, e.g. data:image/png;base64,XXXXX
        int comma = raw.indexOf(',');
        if (raw.regionMatches(true, 0, "data:", 0, 5) && comma > 0) {
            raw = raw.substring(comma + 1);
        }

        // Remove whitespace to be lenient
        raw = raw.replaceAll("\\s+", "");

        // Validate base64 content by decoding; this also yields exact byte size
        byte[] bytes;
        try {
            // MIME decoder tolerates line breaks
            bytes = Base64.getMimeDecoder().decode(raw);
        } catch (IllegalArgumentException ex) {
            addMessage(context, "The provided image is not valid Base64 data.");
            return false;
        }

        long size = bytes.length;
        if (size > maxBytes) {
            addMessage(context, "Image is " + humanMb(size) + " MB; maximum allowed is " + trimTrailingZeros(maxMb) + " MB.");
            return false;
        }
        return true;
    }

    private static void addMessage(ConstraintValidatorContext context, String msg) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(msg).addConstraintViolation();
    }

    private static String humanMb(long bytes) {
        double mb = bytes / 1024.0 / 1024.0;
        return trimTrailingZeros(mb);
    }

    private static String trimTrailingZeros(double val) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", val);
        // Remove trailing zeros and possible trailing decimal point
        s = s.indexOf('.') >= 0 ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s;
    }
}

