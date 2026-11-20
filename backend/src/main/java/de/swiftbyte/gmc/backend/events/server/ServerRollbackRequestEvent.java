package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SERVER_ROLLBACK)
public class ServerRollbackRequestEvent extends ServerEvent implements ActionLogEvent {
    private final String backupId;
    private final String backupName;
    private final boolean rollbackPlayers;

    public ServerRollbackRequestEvent(String issuerId, GameServer server, String backupId, String backupName, boolean rollbackPlayers) { this(issuerId, Instant.now(), server, backupId, backupName, rollbackPlayers); }

    public ServerRollbackRequestEvent(String issuerId, Instant timestamp, GameServer server, String backupId, String backupName, boolean rollbackPlayers) {
        super(issuerId, timestamp, server);
        this.backupId = backupId;
        this.backupName = backupName;
        this.rollbackPlayers = rollbackPlayers;
        String namePart = backupName == null || backupName.isBlank() ? "ID " + backupId : "\"" + backupName + "\" (ID " + backupId + ")";
        setActionLogInformation("Rolled back to backup " + namePart + (rollbackPlayers ? " including players" : " without players"));
    }
}
