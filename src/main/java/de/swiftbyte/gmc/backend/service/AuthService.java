package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.controller.AuthController;
import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.VerificationException;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import de.swiftbyte.gmc.backend.security.ClientIpResolver;
import de.swiftbyte.gmc.backend.security.SessionCookieUtil;
import de.swiftbyte.gmc.backend.util.Generators;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.Instant;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserService userService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private EmailSender emailSender;

    

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Autowired
    private LimitService limitService;

    @Autowired
    private SessionCookieUtil sessionCookieUtil;


    public AuthController.LoginResponse login(AuthController.LoginRequest loginRequest, HttpServletRequest httpRequest) {
        Optional<User> optional = userService.findByEmail(loginRequest.getEmail());
        if (optional.isEmpty()) throw new AuthenticationException("credentials_invalid");
        User user = optional.get();
        // Reject login for disabled users (null means enabled for backwards compatibility)
        if (user.getEnabled() != null && !user.getEnabled())
            throw new AuthenticationException("user_disabled");
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword()))
            throw new AuthenticationException("credentials_invalid");

        if (loginRequest.getTimeUnit() == null || loginRequest.getTimeout() == 0) {
            if (loginRequest.isRememberMe()) {
                loginRequest.setTimeUnit(TimeUnit.DAYS);
                loginRequest.setTimeout(30);
            } else {
                loginRequest.setTimeUnit(TimeUnit.HOURS);
                loginRequest.setTimeout(1);
            }
        }


        twoFactorService.verifyOnLogin(user,
                loginRequest.getTwoFACode(),
                loginRequest.getBackupCode());


        Session session = new Session();
        session.setUserId(user.getId());
        session.setCreated(Instant.now());
        session.setLastAccessed(Instant.now());
        session.setLastIp(clientIpResolver.resolve(httpRequest));
        session.setExpiresAt(Date.from(Instant.now().plusSeconds(loginRequest.getTimeUnit().toSeconds(loginRequest.getTimeout()))));
        session.setLastUserAgent(httpRequest.getHeader("User-Agent"));
        sessionService.save(session);

        if (user.getNotificationSettings() == null)
            user.setNotificationSettings(new User.NotificationSettings().setEmailOnLogin(false));
        if (user.getNotificationSettings().isEmailOnLogin())
            emailSender.sendLoginNotificationEmail(user, session.getLastIp(), session.getLastUserAgent());

        // Update last login timestamp on successful authentication
        user.setLastLogin(Instant.now());
        userService.save(user);


        AuthController.LoginResponse loginResponse = new AuthController.LoginResponse();
        loginResponse.setSessionId(session.getSessionId());
        long timeoutMs = session.getExpiresAt().getTime();
        loginResponse.setTimeout(timeoutMs);
        String humanLeft = humanReadableDuration(timeoutMs - System.currentTimeMillis());
        loginResponse.setMessage("Logged in successfully on account " + user.getEmail() + ". Time left: " + humanLeft + ".");
        return loginResponse;
    }

    private static String humanReadableDuration(long millis) {
        long total = Math.max(0, millis / 1000);
        long days = total / 86400; total %= 86400;
        long hours = total / 3600; total %= 3600;
        long minutes = total / 60; long seconds = total % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    

    public Session register(AuthController.RegisterRequest request, HttpServletRequest httpRequest) {
        if (userService.existsByEmail(request.getEmail())) throw new AuthenticationException("email_already_exists");

        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerificationCode(Generators.generateVerificationToken());
        user.setTwoFAType(User.TwoFAType.NONE);
        user.setAccountType(User.AccountType.USER);
        user.setEnabled(true);
        // Initialize per-user limit defaults
        try {
            // Avoid a hard dependency in case of initialization order; swallow if TeamService not yet ready
            int defaultMaxOwned = limitService.getDefaultMaxOwnedTeamsPerUser();
            user.setMaxOwnedTeams(defaultMaxOwned > 0 ? defaultMaxOwned : null);
        } catch (Exception ignored) {
        }

        User.PersonalInformation info = new User.PersonalInformation();

        info.setUsername(request.getUsername());
        info.setName(request.getName());
        info.setBillingAddress(request.getBillingAddress());

        user.setPersonalInformation(info);
        user.setCompanyInformation(request.getCompanyInformation());

        User.NotificationSettings notificationSettings = new User.NotificationSettings();
        // Keep existing login notifications default
        notificationSettings.setEmailOnLogin(true);
        // Default team email notifications to disabled and threshold ERROR
        notificationSettings.setEmailNotificationsEnabled(false);
        notificationSettings.setEmailNotificationsMinSeverity(de.swiftbyte.gmc.common.model.NotificationSeverity.ERROR);

        user.setNotificationSettings(notificationSettings);

        emailSender.sendVerificationEmail(user);
        userService.save(user);

        // Create a session for the newly registered user and return it
        Session session = new Session();
        session.setUserId(user.getId());
        session.setCreated(Instant.now());
        session.setLastAccessed(Instant.now());
        session.setLastIp(clientIpResolver.resolve(httpRequest));
        // Default to 1 hour session on register
        session.setExpiresAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)));
        session.setLastUserAgent(httpRequest.getHeader("User-Agent"));
        sessionService.save(session);
        return session;
    }

    public void verifyEmail(User user, int verificationToken) {
        if (user.getEmailVerificationCode() != verificationToken) throw new VerificationException("invalid_token");
        user.setEmailVerified(true);
        user.setEmailVerificationCode(0);
        userService.save(user);
    }

    public void resendVerificationEmail(User user) {
        if (user.isEmailVerified()) throw new AuthenticationException("email_already_verified");
        user.setEmailVerificationCode(Generators.generateVerificationToken());
        emailSender.sendVerificationEmail(user);
        userService.save(user);
    }

    public void logout(HttpServletRequest request) {
        String sessionId = sessionCookieUtil.readSessionId(request);
        if (sessionId != null) sessionService.delete(sessionId);
    }

    public void logoutAll(@AuthenticationPrincipal User user) {
        sessionService.deleteAllByUserId(user.getId());
    }

    public List<Session> getSessions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        String currentSessionId = sessionCookieUtil.readSessionId(request);
        List<Session> sessions = sessionService.findByUserId(user.getId());
        sessions.forEach(session -> {
            if (session.getSessionId().equals(currentSessionId)) session.setSessionId("CURRENT");
            else session.setSessionId("HIDDEN");
        });
        return sessions;
    }

    public void resetPassword(String email, String newPassword, String verificationCode) {
        User user = userService.findByEmail(email).orElseThrow(NotFoundException::new);
        if (user.getPasswordResetCode() == null || !verificationCode.equals(user.getPasswordResetCode()))
            throw new AuthenticationException("invalid_reset_token");

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetCode(null);
        userService.save(user);
        emailSender.sendPasswordResetSuccessEmail(user);
    }

    public void requestResetPassword(String email) {
        User user = userService.findByEmail(email).orElseThrow(NotFoundException::new);
        user.setPasswordResetCode(Generators.generatePasswordResetToken());
        emailSender.sendPasswordResetEmail(user);
        userService.save(user);
    }


}
