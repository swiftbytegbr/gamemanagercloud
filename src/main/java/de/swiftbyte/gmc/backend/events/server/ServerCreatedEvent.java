package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.SERVER_CREATION)
public class ServerCreatedEvent extends ServerEvent implements ActionLogEvent {
    public ServerCreatedEvent(String issuerId, GameServer server) { this(issuerId, Instant.now(), server); }

    public ServerCreatedEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp, server);
        setActionLogInformation("Created server \"" + (server.getDisplayName() == null ? server.getId() : server.getDisplayName()) + "\"");
    }
}
