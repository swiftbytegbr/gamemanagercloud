package de.swiftbyte.gmc.backend.infrastructure;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class TotpService {

    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6);

    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank()) return false;
        if (code == null || code.isBlank()) return false;
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        // Allow a small time-window drift (1 step = 30s) for reliability.
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }

    public String buildProvisioningUri(String secret, String accountName, String issuer) {
        // otpauth://totp/{issuer}:{account}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30
        String label = encode(issuer) + ":" + encode(accountName);
        String params = "secret=" + encode(secret)
                + "&issuer=" + encode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
        return "otpauth://totp/" + label + "?" + params;
    }

    private String encode(String val) {
        try {
            return URLEncoder.encode(val, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return val;
        }
    }
}
