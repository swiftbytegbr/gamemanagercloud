package de.swiftbyte.gmc.backend.infrastructure;

import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
public class EmailSender {

    // Optional, so that the application also starts without any SMTP configuration
    @Autowired
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.username:}")
    private String from;

    // GMC_MAIL_ENABLED=false disables all e-mails (verification, 2FA codes, password reset, notifications)
    @Value("${gmc.mail.enabled:true}")
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    /** Returns the mail sender, or null if mail is disabled or not configured. */
    private JavaMailSender mailSender(String to, String subject) {
        if (!enabled) {
            log.debug("Mail disabled, not sending '{}' to {}", subject, to);
            return null;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) log.warn("Mail is enabled but no SMTP server is configured, not sending '{}' to {}", subject, to);
        return sender;
    }

    private void applyFrom(MimeMessageHelper helper) throws jakarta.mail.MessagingException {
        if (from != null && !from.isBlank()) helper.setFrom(from);
    }

    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load email template: " + templateName, e);
        }
    }

    private String fillPlaceholders(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String renderEmail(String contentHtml, Map<String, String> variables) {
        String baseTemplate = loadTemplate("base_email.html");
        String contentFilled = fillPlaceholders(contentHtml, variables);
        return baseTemplate.replace("{{content}}", contentFilled);
    }

    public void sendVerificationEmail(User user) {
        String subject = "Verify your email";
        String contentHtml = loadTemplate("verification.html");
        Map<String, String> vars = Map.of("token", String.valueOf(user.getEmailVerificationCode()));
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendPasswordResetEmail(User user) {
        String subject = "Password Reset";
        String contentHtml = loadTemplate("password_reset.html");
        Map<String, String> vars = Map.of("resetCode", String.valueOf(user.getPasswordResetCode()));
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendPasswordResetSuccessEmail(User user) {
        String subject = "Your password has been changed";
        String contentHtml = loadTemplate("password_reset_success.html");
        String html = renderEmail(contentHtml, Map.of());
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendLoginNotificationEmail(User user, String ip, String userAgent) {
        String subject = "New Login Detected";
        String contentHtml = loadTemplate("login_notification.html");
        Map<String, String> vars = Map.of(
                "ip", ip,
                "userAgent", userAgent
        );
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendTeamInviteEmail(String email, Team team) {
        String subject = "You have been invited to join a team!";
        String contentHtml = loadTemplate("team_invite.html");
        Map<String, String> vars = Map.of(
                "teamName", team.getName(),
                "inviteCode", team.getInviteCode()
        );
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(email, subject, html);
    }

    public void sendTeamNotificationEmail(User user, Team team, NotificationSeverity severity, String title, String message, String createdAtIso) {
        String teamName = team.getName() == null ? "Team" : team.getName();
        String subject = "[" + teamName + "] " + severity + ": " + (title == null ? "" : title);
        String contentHtml = loadTemplate("team_notification.html");
        Map<String, String> vars = Map.of(
                "teamName", teamName,
                "severity", String.valueOf(severity),
                "title", title == null ? "" : title,
                "message", message == null ? "" : message,
                "createdAt", createdAtIso == null ? "" : createdAtIso
        );
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        JavaMailSender mailSender = mailSender(to, subject);
        if (mailSender == null) return;
        new Thread(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                applyFrom(helper);
                mailSender.send(message);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AuthenticationException("send_failed");
            }
        }).start();
    }

    public void send2faEmail(User user) {
        String subject = "Your 2FA Code";
        String contentHtml = loadTemplate("twofa_email.html");
        Map<String, String> vars = Map.of("twoFAToken", String.valueOf(user.getTwoFACode()));
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendTwofaEnabledNotification(User user, String method) {
        String subject = "Two-Factor Authentication Enabled";
        String contentHtml = loadTemplate("twofa_enabled.html");
        Map<String, String> vars = Map.of(
                "method", method,
                "userEmail", user.getEmail()
        );
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendTwofaDisabledNotification(User user, String method) {
        String subject = "Two-Factor Authentication Disabled";
        String contentHtml = loadTemplate("twofa_disabled.html");
        Map<String, String> vars = Map.of(
                "method", method,
                "userEmail", user.getEmail()
        );
        String html = renderEmail(contentHtml, vars);
        sendHtmlEmail(user.getEmail(), subject, html);
    }

    public void sendPlainEmailWithReplyTo(String to, String subject, String body, List<String> replyTo) {
        JavaMailSender mailSender = mailSender(to, subject);
        if (mailSender == null) return;
        new Thread(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                applyFrom(helper);
                helper.setText(body, false);

                if (replyTo != null && !replyTo.isEmpty()) {
                    InternetAddress[] replyToAddresses = replyTo.stream()
                            .filter(addr -> addr != null && !addr.isBlank())
                            .distinct()
                            .map(addr -> {
                                try {
                                    return new InternetAddress(addr);
                                } catch (Exception e) {
                                    return null; // skip invalid addresses
                                }
                            })
                            .filter(a -> a != null)
                            .toArray(InternetAddress[]::new);
                    if (replyToAddresses.length > 0) {
                        message.setReplyTo(replyToAddresses);
                    }
                }

                mailSender.send(message);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AuthenticationException("send_failed");
            }
        }).start();
    }

    public void sendPlainEmailWithReplyTo(String to, String subject, String body, String replyTo) {
        JavaMailSender mailSender = mailSender(to, subject);
        if (mailSender == null) return;
        new Thread(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setTo(to);
                helper.setSubject(subject);
                applyFrom(helper);
                helper.setText(body, false);
                if (replyTo != null && !replyTo.isBlank()) {
                    helper.setReplyTo(replyTo);
                }
                mailSender.send(message);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AuthenticationException("send_failed");
            }
        }).start();
    }
}
