package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.SERVER_UPDATE)
public class ServerUpdateRequestEvent extends ServerEvent implements ActionLogEvent {
    public ServerUpdateRequestEvent(String issuerId, GameServer server) { this(issuerId, Instant.now(), server); }

    public ServerUpdateRequestEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp, server);
        setActionLogInformation("Updated server \"" + (server.getDisplayName() == null ? server.getId() : server.getDisplayName()) + "\"");
    }
}
