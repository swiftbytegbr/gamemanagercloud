package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_INVITATION)
public class TeamInvitationCreatedEvent extends TeamEvent implements ActionLogEvent {
    private final String invitedEmail;

    public TeamInvitationCreatedEvent(String issuerId, Team team, String invitedEmail) { this(issuerId, Instant.now(), team, invitedEmail); }
    public TeamInvitationCreatedEvent(String issuerId, Instant timestamp, Team team, String invitedEmail) {
        super(issuerId, timestamp, team);
        this.invitedEmail = invitedEmail;
        setActionLogInformation("Invited " + invitedEmail + " to the team");
    }
}
