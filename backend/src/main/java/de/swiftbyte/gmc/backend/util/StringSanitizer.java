package de.swiftbyte.gmc.backend.util;

public final class StringSanitizer {

    private StringSanitizer() { }

    /**
     * Returns a version of the input containing only A-Z, a-z, 0-9, hyphen (-), underscore (_), and dot (.).
     * All other characters, including whitespace, are removed. Null yields an empty string.
     */
    public static String emailSafePart(String input) {
        if (input == null) return "";
        return input.replaceAll("[^A-Za-z0-9._-]", "");
    }
}

