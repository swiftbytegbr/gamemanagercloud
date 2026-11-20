package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_APPLICATION_CREATION)
public class TeamApplicationUserCreatedEvent extends TeamEvent implements ActionLogEvent {
    private final String applicationName;

    public TeamApplicationUserCreatedEvent(String issuerId, Team team, String applicationName) { this(issuerId, Instant.now(), team, applicationName); }
    public TeamApplicationUserCreatedEvent(String issuerId, Instant timestamp, Team team, String applicationName) {
        super(issuerId, timestamp, team);
        this.applicationName = applicationName;
        // No additional details in message
    }
}
