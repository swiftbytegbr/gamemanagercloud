package de.swiftbyte.gmc.backend.expressions.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;

public final class AutomationFunctions {
    private AutomationFunctions() {}

    public static String join(Collection<?> items, String sep) {
        if (items == null || items.isEmpty()) return "";
        return String.join(sep == null ? ", " : sep,
                items.stream().map(o -> o == null ? "" : String.valueOf(o)).toList());
    }

    public static String upper(String s) { return s == null ? null : s.toUpperCase(Locale.ROOT); }
    public static String lower(String s) { return s == null ? null : s.toLowerCase(Locale.ROOT); }
    public static String trim(String s) { return s == null ? null : s.trim(); }

    public static String nowIso() { return Instant.now().toString(); }
    public static long epoch() { return Instant.now().getEpochSecond(); }
    public static String formatDate(long epochSeconds, String pattern, String zone) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern == null ? "yyyy-MM-dd HH:mm" : pattern)
                .withZone(ZoneId.of(zone == null ? "UTC" : zone));
        return fmt.format(Instant.ofEpochSecond(epochSeconds));
    }

    public static double min(double a, double b) { return Math.min(a, b); }
    public static double max(double a, double b) { return Math.max(a, b); }
    public static double sum(double a, double b) { return a + b; }
    public static double avg(double a, double b) { return (a + b) / 2.0; }
}

