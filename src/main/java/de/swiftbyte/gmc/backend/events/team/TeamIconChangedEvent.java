package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_ICON_CHANGE)
public class TeamIconChangedEvent extends TeamEvent implements ActionLogEvent {

    public TeamIconChangedEvent(String issuerId, Team team)
    { this(issuerId, Instant.now(), team); }
    public TeamIconChangedEvent(String issuerId, Instant timestamp, Team team) {
        super(issuerId, timestamp, team);
    }
}
