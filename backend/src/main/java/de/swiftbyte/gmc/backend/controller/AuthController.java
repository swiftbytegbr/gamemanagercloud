package de.swiftbyte.gmc.backend.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.security.SessionCookieUtil;
import de.swiftbyte.gmc.backend.service.AuthService;
import de.swiftbyte.gmc.backend.validation.CountryCodeMatch;
import de.swiftbyte.gmc.backend.validation.OptionalSafeName;
import de.swiftbyte.gmc.backend.validation.RequireWith;
import de.swiftbyte.gmc.backend.validation.SafeName;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {


    @Autowired
    private AuthService authService;

    @Autowired
    private SessionCookieUtil sessionCookieUtil;


    @Data
    public static class LoginRequest {
        private String email;
        private String password;
        private String twoFACode; // numeric code as string
        // For backup codes (alphanumeric, shown once); optional.
        private String backupCode;
        private boolean rememberMe;
        private long timeout;
        private TimeUnit timeUnit;
    }

    @Data
    public static class LoginResponse {
        @JsonIgnore
        private String sessionId;
        private long timeout;
        private String message;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest data, HttpServletRequest request, HttpServletResponse response) {
        LoginResponse res = authService.login(data, request);
        // Write cookie; sessionId is not serialized (JsonIgnore)
        if (res.getSessionId() != null) {
            long maxAgeSeconds = Math.max(0, (res.getTimeout() - System.currentTimeMillis()) / 1000);
            sessionCookieUtil.setSessionCookie(response, res.getSessionId(), maxAgeSeconds);
        }
        return res;
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request);
        sessionCookieUtil.clearSessionCookie(response);
    }

    @PostMapping("/logout/all")
    public void logoutAll(@AuthenticationPrincipal User user, HttpServletResponse response) {
        authService.logoutAll(user);
        // Also clear the session cookie of the executing user to avoid stale/invalid session on next request
        sessionCookieUtil.clearSessionCookie(response);
    }

    @GetMapping("/session")
    public List<Session> getSessions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return authService.getSessions(user, request);
    }


    @Data
    @CountryCodeMatch(left = "companyInformation.countryCode", right = "billingAddress.countryCode")
    @RequireWith(left = "companyInformation", right = "billingAddress")
    public static class RegisterRequest {
        @Email
        @NotNull
        String email;
        @NotNull
        String password;
        @OptionalSafeName
        String username;

        @SafeName
        String name;

        @Valid
        User.CompanyInformation companyInformation;

        @Valid
        User.PersonalInformation.Address billingAddress;
    }

    @PostMapping("/register")
    public void register(@RequestBody @Valid RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Session session = authService.register(request, httpRequest);
        if (session != null) {
            long maxAgeSeconds = Math.max(0, (session.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000);
            sessionCookieUtil.setSessionCookie(response, session.getSessionId(), maxAgeSeconds);
        }
    }

    @PostMapping("/register/resend")
    public void resend(@AuthenticationPrincipal User user) {
        authService.resendVerificationEmail(user);
    }

    @Data
    public static class RegisterVerify {
        private Integer token;
    }

    @PostMapping("/register/verify")
    public void verify(@RequestBody RegisterVerify body, @AuthenticationPrincipal User user) {
        authService.verifyEmail(user, body.token);
    }

    @Data
    public static class PasswordResetRequest {
        private String email;
    }

    @PostMapping("/reset-password/request")
    public void requestResetPassword(@RequestBody PasswordResetRequest request) {
        authService.requestResetPassword(request.email);
    }

    @Data
    public static class PasswordResetVerify {
        private String email;
        private String newPassword;
        private String verificationCode;
    }

    @PostMapping("/reset-password/verify")
    public void resetPassword(@RequestBody PasswordResetVerify body) {
        authService.resetPassword(body.email, body.newPassword, body.verificationCode);
    }

}
