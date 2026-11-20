package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.common.entity.GameServerState;
import lombok.Getter;

import java.time.Instant;

/**
 * Emitted when a server's state is observed/updated (e.g., on heartbeat).
 * Carries the new state for event-driven automation waits.
 */

@Getter
public class ServerStateChangedEvent extends ServerEvent {
    private final GameServerState oldState;
    private final GameServerState newState;

    public ServerStateChangedEvent(String issuerId, Instant timestamp, GameServer server, GameServerState newState) {
        super(issuerId, timestamp, server);
        this.oldState = null;
        this.newState = newState;
    }

    public ServerStateChangedEvent(String issuerId, GameServer server, GameServerState newState) {
        super(issuerId, Instant.now(), server);
        this.oldState = null;
        this.newState = newState;
    }

    public ServerStateChangedEvent(String issuerId, Instant timestamp, GameServer server, GameServerState oldState, GameServerState newState) {
        super(issuerId, timestamp, server);
        this.oldState = oldState;
        this.newState = newState;
    }

}
