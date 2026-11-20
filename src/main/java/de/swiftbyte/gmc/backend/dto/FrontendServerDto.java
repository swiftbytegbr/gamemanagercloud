package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.common.entity.Backup;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.entity.RconCommand;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

@Data
@Accessors(chain = true)
public class FrontendServerDto {
    // Original fields mirrored from GameServer entity
    private String id;
    private String displayName;
    private String nodeId;
    private Instant created;
    private String version;
    private GameType gameType;
    private GameServerState state;
    private String settingProfileId;
    private String serverDirectory;
    private int onlinePlayers;
    private List<Backup> backups;
    private List<RconCommand> commands;

    // Enriched info
    private String nodeName;
    private String serverIp;
    private String map;
    private int maxPlayers;
    private int modCount;
    private Integer serverPort;
    private Integer queryPort;
    private Integer rconPort;
}
