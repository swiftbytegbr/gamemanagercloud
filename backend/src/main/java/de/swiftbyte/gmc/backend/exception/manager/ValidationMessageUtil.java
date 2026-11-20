package de.swiftbyte.gmc.backend.exception.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ValidationMessageUtil {

    private static final Map<String, String> SIMPLE_VALIDATION_MAPPINGS = Map.ofEntries(
            Map.entry("NotBlank", "is required"),
            Map.entry("NotNull", "is required"),
            Map.entry("Email", "must be a valid email address"),
            Map.entry("Positive", "must be positive"),
            Map.entry("Past", "must be in the past"),
            Map.entry("Future", "must be in the future")
    );

    public static String toFriendlyPhrase(String fieldPath, String rawMessage, String constraintCode, Object rejected) {
        String fieldLabel = humanizeField(fieldPath);

        // Prefer mapped explanations when available (try prefixed then raw)
        if (rawMessage != null) {
            String prefixed = rawMessage.startsWith("validation.") ? rawMessage : "validation." + rawMessage;
            if (ExceptionMessages.ERROR_EXPLANATIONS.containsKey(prefixed)) {
                return stripTrailingPunctuation(ExceptionMessages.ERROR_EXPLANATIONS.get(prefixed));
            }
            if (ExceptionMessages.ERROR_EXPLANATIONS.containsKey(rawMessage)) {
                return stripTrailingPunctuation(ExceptionMessages.ERROR_EXPLANATIONS.get(rawMessage));
            }
        }

        if (rawMessage != null && rawMessage.startsWith("invalid_")) {
            return fieldLabel + " is invalid";
        }

        if (constraintCode != null && SIMPLE_VALIDATION_MAPPINGS.containsKey(constraintCode)) {
            return fieldLabel + " " + SIMPLE_VALIDATION_MAPPINGS.get(constraintCode);
        }

        if (rawMessage != null && !rawMessage.isBlank()) {
            String rm = rawMessage.trim();
            if (looksLikeSentence(rm)) {
                return stripTrailingPunctuation(rm);
            }
            return fieldLabel + ": " + rm;
        }

        return fieldLabel + " is invalid";
    }

    public static String composeRootMessage(List<String> phrases) {
        if (phrases == null) phrases = new ArrayList<>();
        return joinAsSentence(phrases);
    }

    public static String computeMessageKey(String rawMessage, String constraintCode) {
        if (rawMessage != null && !rawMessage.isBlank()) {
            String m = rawMessage.trim();
            if (!m.startsWith("validation.")) return "validation." + m;
            return m;
        }
        if (constraintCode != null) {
            if (constraintCode.equals("NotBlank") || constraintCode.equals("NotNull")) {
                return "validation.required";
            }
        }
        return "validation.invalid";
    }

    public static String joinAsSentence(List<String> parts) {
        List<String> normalized = parts.stream()
                .map(ValidationMessageUtil::stripTrailingPunctuation)
                .toList();
        String combined;
        if (normalized.isEmpty()) {
            combined = "";
        } else if (normalized.size() == 1) {
            combined = normalized.get(0);
        } else if (normalized.size() == 2) {
            combined = normalized.get(0) + " and " + normalized.get(1);
        } else {
            combined = String.join(", ", normalized.subList(0, normalized.size() - 1)) +
                    ", and " + normalized.get(normalized.size() - 1);
        }
        if (!combined.isEmpty()) {
            combined = combined.substring(0, 1).toUpperCase(Locale.ROOT) + combined.substring(1);
            if (!combined.endsWith(".")) combined = combined + ".";
        }
        return combined;
    }

    public static String ensureSentence(String s) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) return t;
        t = t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
        if (!t.endsWith(".")) t = t + ".";
        return t;
    }

    public static String stripTrailingPunctuation(String s) {
        if (s == null) return "";
        String t = s.trim();
        while (!t.isEmpty() && (t.endsWith(".") || t.endsWith(";") )) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }

    public static boolean looksLikeSentence(String s) {
        String t = s == null ? "" : s.trim();
        return t.endsWith(".") || t.contains(" ");
    }

    public static String humanizeField(String path) {
        if (path == null || path.isBlank()) return "Field";
        String last = path;
        int dot = path.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < path.length()) last = path.substring(dot + 1);

        Map<String, String> known = Map.ofEntries(
                Map.entry("vatNumber", "VAT number"),
                Map.entry("countryCode", "country code"),
                Map.entry("postalCode", "postal code"),
                Map.entry("companyName", "company name"),
                Map.entry("billingAddress", "billing address"),
                Map.entry("email", "email"),
                Map.entry("name", "name"),
                Map.entry("username", "username")
        );
        if (known.containsKey(last)) return known.get(last);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < last.length(); i++) {
            char c = last.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && Character.isLowerCase(last.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(Character.toLowerCase(c));
        }
        String label = sb.toString().trim();
        label = label.replaceAll("\\bid\\b", "ID");
        label = label.replaceAll("\\bvat\\b", "VAT");
        return label;
    }
}
