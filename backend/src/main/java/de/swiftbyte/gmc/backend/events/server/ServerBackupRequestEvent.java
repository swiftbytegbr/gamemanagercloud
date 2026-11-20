package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SERVER_BACKUP)
public class ServerBackupRequestEvent extends ServerEvent implements ActionLogEvent {
    private final String backupName;

    public ServerBackupRequestEvent(String issuerId, GameServer server, String backupName) { this(issuerId, Instant.now(), server, backupName); }

    public ServerBackupRequestEvent(String issuerId, Instant timestamp, GameServer server, String backupName) {
        super(issuerId, timestamp, server);
        this.backupName = backupName;
        setActionLogInformation("Created backup \"" + backupName + "\"");
    }
}
