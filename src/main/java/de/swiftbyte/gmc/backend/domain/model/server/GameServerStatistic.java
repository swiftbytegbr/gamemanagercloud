package de.swiftbyte.gmc.backend.domain.model.server;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@Document(collection = "server_statistics")
public class GameServerStatistic {

    @Id
    private String serverId;

    private Map<Instant, GameServerStatisticData> dataMap;


    @Data
    public static class GameServerStatisticData {
        private int players;
        private long ramBytes;
        private int cpuPercentage;
        private long networkBytes;
    }
}
