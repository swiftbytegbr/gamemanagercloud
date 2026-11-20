package de.swiftbyte.gmc.backend.events.node;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.events.BaseEvent;
import lombok.Getter;

import java.time.Instant;

@Getter
public abstract class NodeEvent extends BaseEvent {
    private final Node node;

    protected NodeEvent(String issuerId, Instant timestamp, Node node) {
        super(issuerId, timestamp);
        this.node = node;
    }
}
