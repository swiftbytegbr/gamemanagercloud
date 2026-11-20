package de.swiftbyte.gmc.backend.util;

import java.security.SecureRandom;
import java.util.Base64;

public class Generators {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static Integer generateVerificationToken() {
        // 6-digit numeric code using a cryptographically secure PRNG
        return 100000 + SECURE_RANDOM.nextInt(900_000);
    }

    public static String generateSessionId() {
        // 256-bit URL-safe random token
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public static String generatePasswordResetToken() {
        // Keep 8-char length for compatibility, but use SecureRandom
        StringBuilder token = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            int idx = SECURE_RANDOM.nextInt(ALPHANUM.length());
            token.append(ALPHANUM.charAt(idx));
        }
        return token.toString();
    }

    public static String generateNodeSecret() {
        // Preserve prefix and use strong URL-safe random bytes
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "gmc-s-" + URL_ENCODER.encodeToString(bytes);
    }

    public static String generateRconPassword() {
        // Preserve prefix and use strong URL-safe random bytes
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return "gmc-rcon-" + URL_ENCODER.encodeToString(bytes);
    }

    public static String generateApplicationSecret() {
        // Strong URL-safe random secret
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }
}
