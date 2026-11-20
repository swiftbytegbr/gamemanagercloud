package de.swiftbyte.gmc.backend.exception;

/**
 * Domain-specific bad request for automation errors.
 * Automatically prefixes keys with "automation." so callers can pass concise keys.
 */
public class AutomationException extends BadRequestException {

    public AutomationException() {
        super("automation.unknown");
    }

    public AutomationException(String key) {
        super(prefix(key));
    }

    // No cause-accepting constructor in BadRequestException; keep to key only

    private static String prefix(String key) {
        if (key == null || key.isBlank()) return "automation.unknown";
        String trimmed = key.trim();
        return trimmed.startsWith("automation.") ? trimmed : "automation." + trimmed;
    }
}
