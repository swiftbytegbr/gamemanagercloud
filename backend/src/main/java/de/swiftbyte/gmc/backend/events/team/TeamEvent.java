package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.BaseEvent;
import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class TeamEvent extends BaseEvent {
    private final Team team;

    protected TeamEvent(String issuerId, Instant timestamp, Team team) {
        super(issuerId, timestamp);
        this.team = team;
    }
}
