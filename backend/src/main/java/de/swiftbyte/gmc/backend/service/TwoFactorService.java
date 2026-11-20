package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.exception.UserException;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import de.swiftbyte.gmc.backend.infrastructure.TotpService;
import de.swiftbyte.gmc.backend.util.Generators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TwoFactorService {

    @Autowired
    private TotpService totpService;

    @Autowired
    private BackupCodeService backupCodeService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailSender emailSender;

    public static class TotpSetup {
        public String secret;
        public String provisioningUri;
        public String issuer;
        public String accountName;
    }

    public static class VerifyResult {
        public List<String> backupCodes; // null if none generated
    }

    // No direct type-change API; enabling/disabling are explicit per-method.

    public TotpSetup setupTotp(User user, String issuer) {
        if (user.getTwoFAType() != null && user.getTwoFAType() != User.TwoFAType.NONE) {
            throw new UserException("twofa_already_enabled");
        }
        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userService.save(user);

        TotpSetup resp = new TotpSetup();
        resp.secret = secret;
        resp.issuer = issuer;
        resp.accountName = user.getEmail();
        resp.provisioningUri = totpService.buildProvisioningUri(secret, user.getEmail(), issuer);
        return resp;
    }

    public VerifyResult verifyTotpAndEnable(User user, String code) {
        if (user.getTwoFAType() != null && user.getTwoFAType() != User.TwoFAType.NONE) {
            throw new UserException("twofa_already_enabled");
        }
        if (!totpService.verifyCode(user.getTotpSecret(), code)) {
            throw new AuthenticationException("invalid_2fa_token");
        }
        user.setTwoFAType(User.TwoFAType.TOTP);
        userService.save(user);
        // Notify user
        emailSender.sendTwofaEnabledNotification(user, "TOTP");

        List<String> codes = backupCodeService.generateIfAbsent(user);
        VerifyResult res = new VerifyResult();
        res.backupCodes = codes.isEmpty() ? null : codes;
        return res;
    }

    public void disableTotp(User user) {
        user.setTwoFAType(User.TwoFAType.NONE);
        user.setTotpSecret(null);
        backupCodeService.clear(user);
        userService.save(user);
        // Notify user
        emailSender.sendTwofaDisabledNotification(user, "TOTP");
    }

    // Enable Email 2FA (exclusive; requires verified email). Returns backup codes if newly created.
    public List<String> enableEmail(User user) {
        if (user.getTwoFAType() != null && user.getTwoFAType() != User.TwoFAType.NONE) {
            throw new UserException("twofa_already_enabled");
        }
        if (!emailSender.isEnabled()) {
            throw new UserException("mail_disabled");
        }
        if (!user.isEmailVerified()) {
            throw new UserException("email_not_verified_for_2fa");
        }
        user.setTwoFAType(User.TwoFAType.EMAIL);
        userService.save(user);
        // Notify user
        emailSender.sendTwofaEnabledNotification(user, "Email");
        return backupCodeService.generateIfAbsent(user);
    }

    // Disable Email 2FA (requires valid email code or backup code in the same request).
    public void disableEmail(User user, String twoFACode, String backupCode) {
        if (user.getTwoFAType() != User.TwoFAType.EMAIL) {
            throw new UserException("twofa_not_email");
        }
        verifyOnLogin(user, twoFACode, backupCode); // may throw 2fa_required or invalid_2fa_token
        user.setTwoFAType(User.TwoFAType.NONE);
        user.setTwoFACode(0);
        backupCodeService.clear(user);
        userService.save(user);
        // Notify user
        emailSender.sendTwofaDisabledNotification(user, "Email");
    }

    public void verifyOnLogin(User user, String twoFACode, String backupCode) {
        if (user.getTwoFAType() == User.TwoFAType.EMAIL) {
            // 1) Prefer checking the email twoFACode first
            if (twoFACode != null && !twoFACode.isBlank()) {
                try {
                    int provided = Integer.parseInt(twoFACode.trim());
                    if (provided == user.getTwoFACode()) {
                        user.setTwoFACode(0);
                        userService.save(user);
                        return;
                    }
                } catch (NumberFormatException ignored) {
                    // fall through to backup/invalid handling
                }
                // If a backup code is also provided, try it next
                if (backupCode != null && !backupCode.isBlank()) {
                    if (backupCodeService.verifyAndConsume(user, backupCode)) return;
                    throw new AuthenticationException("invalid_backup_code");
                }
                throw new AuthenticationException("invalid_2fa_token");
            }

            // 2) If email twoFACode missing, allow backup code; otherwise trigger email flow
            if (backupCode != null && !backupCode.isBlank()) {
                if (backupCodeService.verifyAndConsume(user, backupCode)) return;
                throw new AuthenticationException("invalid_backup_code");
            }

            // Codes cannot be delivered without e-mail; backup codes keep working (see above)
            if (!emailSender.isEnabled()) throw new AuthenticationException("2fa_email_unavailable");

            user.setTwoFACode(Generators.generateVerificationToken());
            emailSender.send2faEmail(user);
            userService.save(user);
            throw new AuthenticationException("2fa_required_email");

        } else if (user.getTwoFAType() == User.TwoFAType.TOTP) {
            // 1) Prefer checking the TOTP code first
            if (twoFACode != null && !twoFACode.isBlank()) {
                String digits = twoFACode.trim();
                if (digits.length() < 6) {
                    try { digits = String.format("%06d", Integer.parseInt(digits)); } catch (NumberFormatException ignored) {}
                }
                if (totpService.verifyCode(user.getTotpSecret(), digits)) return;

                // If a backup code is also provided, try it next
                if (backupCode != null && !backupCode.isBlank()) {
                    if (backupCodeService.verifyAndConsume(user, backupCode)) return;
                    throw new AuthenticationException("invalid_backup_code");
                }
                throw new AuthenticationException("invalid_2fa_token");
            }

            // 2) If TOTP code missing, allow backup code; otherwise require TOTP
            if (backupCode != null && !backupCode.isBlank()) {
                if (backupCodeService.verifyAndConsume(user, backupCode)) return;
                throw new AuthenticationException("invalid_backup_code");
            }
            throw new AuthenticationException("2fa_required_totp");
        }
    }
}
