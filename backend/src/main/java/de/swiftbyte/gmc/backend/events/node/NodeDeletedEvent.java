package de.swiftbyte.gmc.backend.events.node;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

@ActionLogType(ActionLogItem.Type.NODE_DELETION)
public class NodeDeletedEvent extends NodeEvent implements ActionLogEvent {
    public NodeDeletedEvent(String issuerId, Node node) { this(issuerId, Instant.now(), node); }
    public NodeDeletedEvent(String issuerId, Instant timestamp, Node node) {
        super(issuerId, timestamp, node);
        setActionLogInformation("Deleted node \"" + node.getSettings().getName() + "\"");
    }
}
