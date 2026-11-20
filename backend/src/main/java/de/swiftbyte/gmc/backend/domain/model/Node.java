package de.swiftbyte.gmc.backend.domain.model;


import de.swiftbyte.gmc.common.entity.NodeData;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Accessors(chain = true)
@Document("nodes")
@CompoundIndexes({
        @CompoundIndex(name = "idx_node_settings_name", def = "{ 'settings.name': 1 }"),
        @CompoundIndex(name = "idx_status_lastAlive", def = "{ 'status': 1, 'lastAlive': 1 }")
})
public class Node {

    @Id
    private String id;

    @Indexed
    private String teamId;

    @Indexed
    private int inviteToken;

    private String secret;

    private String daemonVersion;

    private Instant lastAlive;
    private Instant lastLogin;

    private String logoutReason;

    @Indexed
    private Status status;
    private NodeSettings settings;
    private NodeData systemData;

    public enum Status {
        ONLINE,
        OFFLINE,
        CREATION
    }
}
