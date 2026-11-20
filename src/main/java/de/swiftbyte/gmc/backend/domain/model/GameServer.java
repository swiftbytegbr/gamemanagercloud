package de.swiftbyte.gmc.backend.domain.model;

import de.swiftbyte.gmc.common.entity.Backup;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.entity.RconCommand;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Data
@CompoundIndexes({
        // Enforce unique displayName per team for fast lookups and rename checks
        @CompoundIndex(name = "uniq_team_displayName", def = "{ 'teamId': 1, 'displayName': 1 }", unique = true)
})
@Document(collection = "server")
public class GameServer {

    @Id
    private String id;

    private String displayName;

    // Denormalized for efficient queries and limits without scanning nodes
    @Indexed
    private String teamId;

    @Indexed
    private String nodeId;

    private Instant created;

    @Indexed
    private GameType gameType;

    private String version;

    private GameServerState state;

    @Indexed
    private String settingProfileId;


    // --- Server Data ---

    // Absolute/normalized directory on node's filesystem where this server is installed
    private String serverDirectory;

    private int onlinePlayers;

    private List<Backup> backups = new ArrayList<>(); // Max 10

    private List<RconCommand> commands = new ArrayList<>(); // Max 30


}
