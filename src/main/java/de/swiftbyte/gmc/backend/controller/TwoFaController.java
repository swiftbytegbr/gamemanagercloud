package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/@me/2fa")
@Validated
public class TwoFaController {

    @Autowired
    private TwoFactorService twoFactorService;

    private static final String ISSUER = "GMC";

    @Data
    public static class SetupResponse {
        private String secret;
        private String provisioningUri;
        private String issuer;
        private String accountName;
    }

    @PostMapping("/totp/setup")
    public SetupResponse setup(@AuthenticationPrincipal User user) {
        var setup = twoFactorService.setupTotp(user, ISSUER);
        SetupResponse resp = new SetupResponse();
        resp.setSecret(setup.secret);
        resp.setIssuer(setup.issuer);
        resp.setAccountName(setup.accountName);
        resp.setProvisioningUri(setup.provisioningUri);
        return resp;
    }

    @Data
    public static class VerifyRequest {
        private String code; // numeric TOTP code as string
    }

    @Data
    public static class VerifyResponse {
        private boolean backupCodesGenerated;
        private List<String> backupCodes; // present only once when generated
    }

    @PostMapping("/totp/verify")
    public VerifyResponse verify(@AuthenticationPrincipal User user, @RequestBody @Valid VerifyRequest body) {
        var result = twoFactorService.verifyTotpAndEnable(user, body.getCode());
        VerifyResponse resp = new VerifyResponse();
        boolean generated = result.backupCodes != null && !result.backupCodes.isEmpty();
        resp.setBackupCodesGenerated(generated);
        resp.setBackupCodes(generated ? result.backupCodes : null);
        return resp;
    }

    @Data
    public static class DisableRequest {
        // Provide one of these to authorize disabling 2FA
        private String twoFACode; // numeric (email/totp) as string
        private String backupCode; // backup code
    }

    @PostMapping("/totp/disable")
    public void disable(@AuthenticationPrincipal User user, @RequestBody @Valid DisableRequest req) {
        twoFactorService.verifyOnLogin(user, req.getTwoFACode(), req.getBackupCode());
        twoFactorService.disableTotp(user);
    }

    // --- Email 2FA ---

    @Data
    public static class EmailEnableResponse {
        private boolean backupCodesGenerated;
        private List<String> backupCodes;
    }

    @PostMapping("/email/enable")
    public EmailEnableResponse enableEmail(@AuthenticationPrincipal User user) {
        var codes = twoFactorService.enableEmail(user);
        EmailEnableResponse resp = new EmailEnableResponse();
        boolean generated = codes != null && !codes.isEmpty();
        resp.setBackupCodesGenerated(generated);
        resp.setBackupCodes(generated ? codes : null);
        return resp;
    }

    @Data
    public static class EmailDisableRequest {
        private String twoFACode; // numeric as string
        private String backupCode; // backup
    }

    @PostMapping("/email/disable")
    public void disableEmail(@AuthenticationPrincipal User user, @RequestBody @Valid EmailDisableRequest req) {
        twoFactorService.disableEmail(user, req.getTwoFACode(), req.getBackupCode());
    }
}
