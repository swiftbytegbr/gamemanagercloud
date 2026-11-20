package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.common.entity.NodeData;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;

@Data
@Accessors(chain = true)
public class NodeDto {

    private String id;

    private String teamId;

    private int inviteToken;

    private String daemonVersion;

    private Instant lastAlive;
    private Instant lastLogin;

    private String logoutReason;

    private Node.Status status;
    private NodeSettings settings;
    private NodeData systemData;

}
