package de.swiftbyte.gmc.backend.events.settings;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import de.swiftbyte.gmc.backend.events.server.ServerEvent;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.SETTING_PROFILE_UPDATE)
public class SettingProfileUpdatedEvent extends ServerEvent implements ActionLogEvent {

    public SettingProfileUpdatedEvent(String issuerId, GameServer server) {
        this(issuerId, Instant.now(), server);
    }


    public SettingProfileUpdatedEvent(String issuerId, Instant timestamp, GameServer server) {
        super(issuerId, timestamp, server);
        setActionLogInformation("Updated setting profile for server " + server.getDisplayName());
    }
}
