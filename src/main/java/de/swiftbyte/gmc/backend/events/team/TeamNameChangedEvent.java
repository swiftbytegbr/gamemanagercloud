package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_NAME_CHANGE)
public class TeamNameChangedEvent extends TeamEvent implements ActionLogEvent {
    private final String oldName;
    private final String newName;

    public TeamNameChangedEvent(String issuerId, Team team, String oldName, String newName) { this(issuerId, Instant.now(), team, oldName, newName); }
    public TeamNameChangedEvent(String issuerId, Instant timestamp, Team team, String oldName, String newName) {
        super(issuerId, timestamp, team);
        this.oldName = oldName;
        this.newName = newName;
    }
}
