package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SERVER_RCON_COMMAND)
public class ServerRconCommandRequestEvent extends ServerEvent implements ActionLogEvent {
    private final String command;

    public ServerRconCommandRequestEvent(String issuerId, GameServer server, String command) { this(issuerId, Instant.now(), server, command); }

    public ServerRconCommandRequestEvent(String issuerId, Instant timestamp, GameServer server, String command) {
        super(issuerId, timestamp, server);
        this.command = command;
        setActionLogInformation("Sent RCON command: '" + command + "'");
    }
}
