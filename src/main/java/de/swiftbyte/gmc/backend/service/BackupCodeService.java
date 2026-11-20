package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class BackupCodeService {

    private static final int DEFAULT_COUNT = 10;
    private static final SecureRandom RAND = new SecureRandom();

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    /**
     * Generate backup codes if none exist, hash and store them on the user.
     * Returns plaintext codes exactly once. Subsequent calls return an empty list.
     */
    public List<String> generateIfAbsent(User user) {
        if (user.getBackupCodeHashes() != null && !user.getBackupCodeHashes().isEmpty()) {
            return List.of();
        }
        List<String> codes = new ArrayList<>(DEFAULT_COUNT);
        List<String> hashes = new ArrayList<>(DEFAULT_COUNT);

        for (int i = 0; i < DEFAULT_COUNT; i++) {
            String code = generateReadableCode();
            codes.add(code);
            hashes.add(passwordEncoder.encode(code));
        }

        user.setBackupCodeHashes(hashes);
        user.setBackupCodesCreatedAt(Instant.now());
        userService.save(user);
        return codes;
    }

    /**
     * Verifies the candidate against stored backup code hashes and consumes it on success.
     */
    public boolean verifyAndConsume(User user, String candidate) {
        if (candidate == null || candidate.isBlank()) return false;
        if (user.getBackupCodeHashes() == null || user.getBackupCodeHashes().isEmpty()) return false;

        List<String> hashes = new ArrayList<>(user.getBackupCodeHashes());
        for (int i = 0; i < hashes.size(); i++) {
            String hash = hashes.get(i);
            if (passwordEncoder.matches(candidate.trim(), hash)) {
                // consume this code
                hashes.remove(i);
                user.setBackupCodeHashes(hashes);
                userService.save(user);
                return true;
            }
        }
        return false;
    }

    /**
     * Clear all stored backup codes (e.g., when disabling 2FA).
     */
    public void clear(User user) {
        user.setBackupCodeHashes(null);
        user.setBackupCodesCreatedAt(null);
        userService.save(user);
    }

    // Generates a 12-char code grouped as XXXX-XXXX-XXXX (hex), 48 bits entropy
    private static String generateReadableCode() {
        byte[] bytes = new byte[6]; // 48 bits
        RAND.nextBytes(bytes);
        String hex = bytesToHex(bytes);
        return (hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12)).toUpperCase(Locale.ROOT);
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}

