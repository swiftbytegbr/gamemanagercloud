package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.SERVER_START)
public class ServerStartRequestEvent extends ServerEvent implements ActionLogEvent {
    public ServerStartRequestEvent(String issuerId, GameServer server) { this(issuerId, Instant.now(), server); }

    public ServerStartRequestEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp, server);
        setActionLogInformation("Started server \"" + (server.getDisplayName() == null ? server.getId() : server.getDisplayName()) + "\"");
    }
}
