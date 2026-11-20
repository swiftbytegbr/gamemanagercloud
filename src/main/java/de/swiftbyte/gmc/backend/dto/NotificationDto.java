package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;
import lombok.Data;

import java.time.Instant;

@Data
public class NotificationDto {
    private String id;
    private String teamId;
    private String type;
    private NotificationSeverity severity;
    private String title;
    private String message;
    private NotificationSource source;
    private Instant createdAt;
    private boolean read;
}
