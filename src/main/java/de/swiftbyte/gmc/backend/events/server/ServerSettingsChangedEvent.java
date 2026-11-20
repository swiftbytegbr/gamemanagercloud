package de.swiftbyte.gmc.backend.events.server;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SERVER_SETTINGS_CHANGE)
public class ServerSettingsChangedEvent extends ServerEvent implements ActionLogEvent {
    private final String description;

    public ServerSettingsChangedEvent(String issuerId, GameServer server, String description) { this(issuerId, Instant.now(), server, description); }

    public ServerSettingsChangedEvent(String issuerId, Instant timestamp, GameServer server, String description) {
        super(issuerId, timestamp, server);
        this.description = description;
        setActionLogInformation(description);
    }
}
