package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.TEAM_CREATION)
public class TeamCreatedEvent extends TeamEvent implements ActionLogEvent {
    public TeamCreatedEvent(String issuerId, Team team) { this(issuerId, Instant.now(), team); }
    public TeamCreatedEvent(String issuerId, Instant timestamp, Team team) {
        super(issuerId, timestamp, team);
    }
}
