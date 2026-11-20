package de.swiftbyte.gmc.backend.validation;

public final class ServerDirectoryPathValidatorUtil {
    private ServerDirectoryPathValidatorUtil() {}

    public static boolean isValidPath(String path) {
        if (path == null) return false;
        // Reject control characters and Windows-forbidden characters
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c < 0x20 || c == 0x7F) return false;
            switch (c) {
                case '<': case '>': case '"': case '|': case '?': case '*':
                    return false;
            }
        }

        String trimmed = path.trim();
        if (trimmed.isEmpty()) return false;

        // If a colon is present, enforce Windows drive pattern like C:\\ or C:/
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            if (!trimmed.matches("^[A-Za-z]:[\\/].*")) return false;
        }

        // Allow UNC paths: \\server\share\...
        if (trimmed.startsWith("\\\\")) {
            String withoutPrefix = trimmed.substring(2);
            if (!withoutPrefix.contains("\\")) return false; // missing share separator
        }

        // For everything else, accept typical Unix/relative paths using / or \
        // Disallow trailing spaces or dots on last segment for Windows compatibility
        String last = trimmed.replace('\\', '/');
        int idx = last.lastIndexOf('/');
        String lastSeg = idx >= 0 ? last.substring(idx + 1) : last;
        if (lastSeg.endsWith(" ") || lastSeg.endsWith(".")) return false;

        // Length sanity
        if (trimmed.length() > 4096) return false;

        return true;
    }
}
