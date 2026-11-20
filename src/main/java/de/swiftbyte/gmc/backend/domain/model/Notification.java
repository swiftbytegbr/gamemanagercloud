package de.swiftbyte.gmc.backend.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@Document(collection = "notifications")
@CompoundIndexes({
        @CompoundIndex(name = "idx_team_created_desc", def = "{ 'teamId': 1, 'createdAt': -1 }")
})
public class Notification {

    @Id
    private String id;

    @Indexed
    private String teamId;

    private String type; // e.g., SERVER_CRASHED

    private NotificationSeverity severity; // INFO | WARNING | ERROR | CRITICAL

    private String title;

    private String message;

    private NotificationSource source; // BACKEND | DAEMON

    @Indexed
    private Instant createdAt;


    // userIds that marked as read
    private List<String> readByIds;

}
