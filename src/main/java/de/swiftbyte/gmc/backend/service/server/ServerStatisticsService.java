package de.swiftbyte.gmc.backend.service.server;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.server.GameServerStatistic;
import de.swiftbyte.gmc.backend.exception.ServerException;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.server.ServerStatisticsRepository;
import de.swiftbyte.gmc.backend.service.LimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
public class ServerStatisticsService {


    @Autowired
    private ServerStatisticsRepository statisticsRepository;
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private LimitService limitService;
    @Value("${gmc.server.statistics.max-size:100}")
    private int defaultMaxStatistics;

    // Minimum interval between persisted statistics entries, configurable via YAML
    @Value("${gmc.server.statistics.min-interval-seconds:60}")
    private long minIntervalSeconds;

    @Async
    public void saveStatistic(String serverId, int players, long ramBytes, int cpuPercentage, long networkBytes) {
        GameServerStatistic stat = statisticsRepository.findById(serverId).orElseGet(() -> {
            GameServerStatistic s = new GameServerStatistic();
            s.setServerId(serverId);
            s.setDataMap(new TreeMap<>());
            return s;
        });
        Map<Instant, GameServerStatistic.GameServerStatisticData> dataMap = stat.getDataMap();
        // Normalize to a navigable (sorted) map to ensure deterministic ordering operations
        NavigableMap<Instant, GameServerStatistic.GameServerStatisticData> navigableMap;
        if (dataMap instanceof NavigableMap) {
            navigableMap = (NavigableMap<Instant, GameServerStatistic.GameServerStatisticData>) dataMap;
        } else if (dataMap != null) {
            navigableMap = new TreeMap<>(dataMap);
        } else {
            navigableMap = new TreeMap<>();
        }
        // Ensure the entity holds the navigable map instance (Mongo may deserialize to LinkedHashMap)
        stat.setDataMap(navigableMap);
        Instant now = Instant.now();
        // Only add if last entry is older than configured minimum interval
        if (!navigableMap.isEmpty()) {
            Instant last = navigableMap.lastKey();
            if (now.minusSeconds(minIntervalSeconds).isBefore(last)) {
                return;
            }
        }
        GameServerStatistic.GameServerStatisticData data = new GameServerStatistic.GameServerStatisticData();
        data.setPlayers(players);
        data.setCpuPercentage(cpuPercentage);
        data.setNetworkBytes(networkBytes);
        data.setRamBytes(ramBytes);
        navigableMap.put(now, data);
        // Determine team-specific statistics cap
        int maxStatistics = resolveTeamStatisticsLimit(serverId);
        // Keep only the latest configured number of entries
        while (maxStatistics > 0 && navigableMap.size() > maxStatistics) {
            navigableMap.pollFirstEntry();
        }
        statisticsRepository.save(stat);
    }

    private int resolveTeamStatisticsLimit(String serverId) {
        try {
            GameServer server = serverRepository.findById(serverId).orElse(null);
            if (server == null) return defaultMaxStatistics;
            Node node = nodeRepository.findById(server.getNodeId()).orElse(null);
            if (node == null) return defaultMaxStatistics;
            return limitService.getTeamLimit(node.getTeamId(), LimitService.LimitType.STATISTICS);
        } catch (Exception ignored) {
            return defaultMaxStatistics;
        }
    }

    public GameServerStatistic getStatistics(String serverId) {
        return statisticsRepository.findById(serverId).orElse(null);
    }

    public GameServerStatistic getOrThrow(String serverId) {
        return statisticsRepository.findById(serverId).orElseThrow(() -> new ServerException("statics_not_found"));
    }
}
