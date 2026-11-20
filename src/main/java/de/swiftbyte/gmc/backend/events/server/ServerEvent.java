package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.BaseEvent;
import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class ServerEvent extends BaseEvent {
    private final GameServer server;

    protected ServerEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp);
        this.server = server;
    }
}
