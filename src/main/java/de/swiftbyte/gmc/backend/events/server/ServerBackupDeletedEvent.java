package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SERVER_BACKUP_DELETION)
public class ServerBackupDeletedEvent extends ServerEvent implements ActionLogEvent {
    private final String backupId;
    private final String backupName;

    public ServerBackupDeletedEvent(String issuerId, GameServer server, String backupId, String backupName) { this(issuerId, Instant.now(), server, backupId, backupName); }

    public ServerBackupDeletedEvent(String issuerId, Instant timestamp, GameServer server, String backupId, String backupName) {
        super(issuerId, timestamp, server);
        this.backupId = backupId;
        this.backupName = backupName;
        setActionLogInformation("Deleted backup " + backupName);
    }
}
