package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_INVITATION_REVOCATION)
public class TeamInvitationRevokedEvent extends TeamEvent implements ActionLogEvent {
    private final String affectedUserId;

    public TeamInvitationRevokedEvent(String issuerId, Team team, String affectedUserId) {
        this(issuerId, Instant.now(), team, affectedUserId);
    }

    public TeamInvitationRevokedEvent(String issuerId, Instant timestamp, Team team, String affectedUserId) {
        super(issuerId, timestamp, team);
        this.affectedUserId = affectedUserId;
        // No additional details; type + issuer + affected user suffice
    }
}
