package de.swiftbyte.gmc.backend.domain.model;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.validation.DiscordWebhookUrl;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.backend.validation.Base64Image;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Data
@Document(collection = "teams")
public class Team {

    @Id
    private String id;

    @Indexed
    private String name;

    @Base64Image(maxMegabytes = 5)
    private String iconBase64;

    // New: reference to stored image document
    private String iconImageId;

    @Indexed
    private String ownerId;

    private HashMap<String, List<Permission>> members;

    private HashMap<String, List<Permission>> applications;

    // Map of invited userId -> invitation timestamp
    private HashMap<String, Instant> invitedMembers;

    @Indexed
    private String inviteCode;

    // Action log has been moved into its own collection

    // --- Monetization/Limit Fields (per team) ---
    private Limits limits;

    private NotificationSettings notificationSettings;


    @Data
    public static class Limits {
        // If any of these are null, service layer falls back to Admin config defaults
        private Integer maxNodes;
        private Integer maxServers;
        private Integer maxBackups;
        private Integer maxRconCommands;
        private Integer maxStatisticsSize;
        private Integer maxMembers;
    }

    // Team-level notification settings (Discord forwarding)
    @Data
    public static class NotificationSettings {
        // Full Discord webhook URL; null/blank disables forwarding
        @DiscordWebhookUrl
        private String discordWebhookUrl;
        // Minimum severity to forward; null defaults to ERROR
        private NotificationSeverity discordNotificationsMinSeverity;
    }


}
