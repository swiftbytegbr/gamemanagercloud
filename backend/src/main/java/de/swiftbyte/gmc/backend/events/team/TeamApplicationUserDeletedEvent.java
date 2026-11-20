package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_APPLICATION_DELETION)
public class TeamApplicationUserDeletedEvent extends TeamEvent implements ActionLogEvent {
    private final String applicationUserId;

    public TeamApplicationUserDeletedEvent(String issuerId, Team team, String applicationUserId) { this(issuerId, Instant.now(), team, applicationUserId); }
    public TeamApplicationUserDeletedEvent(String issuerId, Instant timestamp, Team team, String applicationUserId) {
        super(issuerId, timestamp, team);
        this.applicationUserId = applicationUserId;
    }

    public String getAffectedUserId() { return applicationUserId; }
}
