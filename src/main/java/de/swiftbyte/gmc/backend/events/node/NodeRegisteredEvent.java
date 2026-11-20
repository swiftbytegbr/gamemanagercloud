package de.swiftbyte.gmc.backend.events.node;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;

import java.time.Instant;

/**
 * Emitted when a node registers with a valid invite token.
 * No issuer context (node itself), so issuerId is null.
 */
@ActionLogType(ActionLogItem.Type.NODE_REGISTRATION)
public class NodeRegisteredEvent extends NodeEvent implements ActionLogEvent {
    public NodeRegisteredEvent(Node node) { this(null, Instant.now(), node); }
    public NodeRegisteredEvent(String issuerId, Instant timestamp, Node node) {
        super(issuerId, timestamp, node);
        setActionLogInformation("Registered node \"" + node.getSettings().getName() + "\"");
    }
}
