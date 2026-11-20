package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import lombok.Data;

import java.time.Instant;

@Data
public class ServerAdminDto {
    private String id;
    private String displayName;
    private String teamId;
    private String nodeId;
    private Instant created;
    private GameType gameType;
    private GameServerState state;
    private int onlinePlayers;
}

