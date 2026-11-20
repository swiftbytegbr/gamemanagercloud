package de.swiftbyte.gmc.backend.events.node;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.NODE_UPDATE)
public class NodeUpdateRequestEvent extends NodeEvent implements ActionLogEvent {
    public NodeUpdateRequestEvent(String issuerId, Node node) { this(issuerId, Instant.now(), node); }
    public NodeUpdateRequestEvent(String issuerId, Instant timestamp, Node node) {
        super(issuerId, timestamp, node);
    }
}
