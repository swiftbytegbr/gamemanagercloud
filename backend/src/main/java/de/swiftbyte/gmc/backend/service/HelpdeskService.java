package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.exception.BadRequestException;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class HelpdeskService {

    @Value("${gmc.helpdesk.email.to:contact@swiftbyte.de}")
    private String helpdeskRecipient;

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailSender emailSender;

    public void submitBugReport(String subject, String message, String teamId, String discordUsername, User reporter) {
        if (helpdeskRecipient == null || helpdeskRecipient.isBlank()) {
            throw new BadRequestException("helpdesk_recipient_not_configured");
        }
        if (!emailSender.isEnabled()) {
            throw new BadRequestException("mail_disabled");
        }

        Team team = teamService.getOrThrow(teamId);

        boolean reporterIsOwner = team.getOwnerId() != null && team.getOwnerId().equals(reporter.getId());
        String ownerEmail = null;
        if (!reporterIsOwner && team.getOwnerId() != null) {
            ownerEmail = userService.findById(team.getOwnerId()).map(User::getEmail).orElse(null);
        }

        // Build minimal, non-sensitive info (as in previous Discord embed)
        StringBuilder sb = new StringBuilder();
        sb.append("Subject: ").append(subject).append("\n");
        sb.append("Reporter: ").append(nvl(reporter.getEmail(), "-")).append("\n");
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            sb.append("Team Owner: ").append(ownerEmail).append("\n");
        }
        if (discordUsername != null && !discordUsername.isBlank()) {
            sb.append("Discord: ").append(discordUsername).append("\n");
        }
        sb.append("\n---\n\n");
        sb.append(message);

        // Set Reply-To only to the reporter
        String replyTo = reporter.getEmail();
        emailSender.sendPlainEmailWithReplyTo(helpdeskRecipient, subject, sb.toString(), replyTo);
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
