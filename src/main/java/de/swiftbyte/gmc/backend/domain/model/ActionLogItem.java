package de.swiftbyte.gmc.backend.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@Document(collection = "team_action_log")
@CompoundIndexes({
        @CompoundIndex(name = "idx_team_timestamp_desc", def = "{ 'teamId': 1, 'timestamp': -1 }")
})
public class ActionLogItem {

    @Id
    private String id;

    private String teamId;

    private Type type;
    private Instant timestamp;
    private String issuerId;
    private String affectedNodeId;
    private String affectedServerId;
    private String affectedUserId;
    private String actionInformation;

    public enum Type {
        SERVER_CREATION,
        SERVER_DELETION,
        SERVER_START,
        SERVER_STOP,
        SERVER_RESTART,
        SERVER_RCON_COMMAND,
        SERVER_BACKUP,
        SERVER_BACKUP_DELETION,
        SERVER_ROLLBACK,
        SERVER_UPDATE,
        SERVER_SETTINGS_CHANGE,
        NODE_CREATION,
        NODE_REGISTRATION,
        NODE_DELETION,
        NODE_SETTINGS_CHANGE,
        NODE_UPDATE,
        TEAM_CREATION,
        TEAM_INVITATION,
        TEAM_INVITATION_REVOCATION,
        TEAM_JOIN,
        TEAM_LEAVE,
        TEAM_KICK,
        TEAM_PERMISSION_CHANGE,
        TEAM_NAME_CHANGE,
        TEAM_ICON_CHANGE,
        TEAM_APPLICATION_CREATION,
        SETTING_PROFILE_UPDATE, TEAM_APPLICATION_DELETION
    }

    public ActionLogItem(Type type) {
        this.type = type;
        this.timestamp = Instant.now();
    }
}
