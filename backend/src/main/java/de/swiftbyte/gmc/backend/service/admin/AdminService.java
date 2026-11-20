package de.swiftbyte.gmc.backend.service.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.config.GmcConfig;
import de.swiftbyte.gmc.backend.domain.value.LimitsConfig;
import de.swiftbyte.gmc.backend.dto.AdminStatsDto;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.ConfigRepository;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeUpdatePacket;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AdminService {

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private StompSender stompSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private MongoTemplate mongoTemplate;


    public static class GmcVersion {
        public String version;
        public String description;
        public String changelog;
        public Instant releaseDate;
    }

    // LimitsConfig moved to domain.model.config

    public void publishGmcVersion(GmcVersion version) {
        configRepository.save(new GmcConfig().setId("gmcVersion").setValue(version));
    }

    public GmcVersion getGmcVersion() {
        return objectMapper.convertValue(configRepository.findById("gmcVersion").orElse(new GmcConfig().setValue(new GmcVersion())).getValue(), GmcVersion.class);
    }


    public String getDaemonVersion() {
        return configRepository.findById("daemonVersion").orElse(new GmcConfig().setValue("0.0.0")).getValue().toString();
    }


    public void publishDaemonVersion(String version) {
        configRepository.save(new GmcConfig().setId("daemonVersion").setValue(version));
        NodeUpdatePacket packet = new NodeUpdatePacket();
        packet.setVersion(getDaemonVersion());
        packet.setAutoUpdate(true);
        stompSender.send("/node/update", packet);
    }

    public LimitsConfig getLimitsConfig() {
        return objectMapper.convertValue(
                configRepository.findById("defaultLimits").orElse(new GmcConfig().setValue(defaultLimits())).getValue(),
                LimitsConfig.class
        );
    }

    public void updateLimitsConfig(LimitsConfig limits) {
        configRepository.save(new GmcConfig().setId("defaultLimits").setValue(limits));
    }

    // Built-in defaults, used when the defaultLimits document is created (0 = unlimited).
    // Can be changed via the admin API or with GMC_LIMIT_* environment variables.
    public LimitsConfig defaultLimits() {
        LimitsConfig limits = new LimitsConfig();
        // Statistics of a server are stored in one document, so they must be bounded
        limits.serverStatisticsMaxSize = 200;
        return limits;
    }

    public AdminStatsDto getSystemStats() {
        AdminStatsDto dto = new AdminStatsDto();
        dto.setTotalUsers(userRepository.count());
        dto.setTotalTeams(teamRepository.count());
        dto.setTotalNodes(nodeRepository.count());
        dto.setTotalServers(serverRepository.count());

        // Aggregate sum of onlinePlayers across all servers
        GroupOperation group = Aggregation.group().sum("onlinePlayers").as("total");
        Aggregation agg = Aggregation.newAggregation(group);
        AggregationResults<SumHolder> res = mongoTemplate.aggregate(agg, mongoTemplate.getCollectionName(GameServer.class), SumHolder.class);
        long totalPlayers = 0L;
        if (res.getUniqueMappedResult() != null && res.getUniqueMappedResult().total != null) {
            totalPlayers = res.getUniqueMappedResult().total;
        }
        dto.setTotalPlayersOnline(totalPlayers);
        return dto;
    }

    private static class SumHolder {
        public Long total;
    }


    @Data
    public static class GmcAdminDevDeploymentAccess {
        private String devDeploymentUrl;
        private String applicationId;
        private String applicationSecret;
    }

    public void updateGmcAdminDevDeploymentAccess(GmcAdminDevDeploymentAccess access) {
        configRepository.save(new GmcConfig().setId("gmcAdminDevDeploymentAccess").setValue(access));
    }

    public GmcAdminDevDeploymentAccess getGmcAdminDevDeploymentAccess() {
        return objectMapper.convertValue(
                configRepository.findById("gmcAdminDevDeploymentAccess").orElse(new GmcConfig().setValue(new GmcAdminDevDeploymentAccess())).getValue(),
                GmcAdminDevDeploymentAccess.class
        );
    }

}
