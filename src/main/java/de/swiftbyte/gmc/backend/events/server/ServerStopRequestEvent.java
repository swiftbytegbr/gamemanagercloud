package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.SERVER_STOP)
public class ServerStopRequestEvent extends ServerEvent implements ActionLogEvent {
    private final boolean forceKill;

    public ServerStopRequestEvent(String issuerId, GameServer server, boolean forceKill) { this(issuerId, Instant.now(), server, forceKill); }

    public ServerStopRequestEvent(String issuerId, Instant timestamp, GameServer server, boolean forceKill) {
        super(issuerId, timestamp, server);
        this.forceKill = forceKill;
        String name = server.getDisplayName() == null ? server.getId() : server.getDisplayName();
        setActionLogInformation((forceKill ? "Killed" : "Stopped") + " server \"" + name + "\"");
    }
}
