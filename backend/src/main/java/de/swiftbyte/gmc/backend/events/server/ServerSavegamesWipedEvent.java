package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.SERVER_SETTINGS_CHANGE)
public class ServerSavegamesWipedEvent extends ServerEvent implements ActionLogEvent {

    public ServerSavegamesWipedEvent(String issuerId, GameServer server) {
        this(issuerId, Instant.now(), server);
    }

    public ServerSavegamesWipedEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp, server);
        setActionLogInformation("Wiped all savegames");
    }
}

