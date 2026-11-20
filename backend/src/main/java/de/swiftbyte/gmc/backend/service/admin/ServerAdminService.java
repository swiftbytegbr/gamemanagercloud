package de.swiftbyte.gmc.backend.service.admin;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.dto.ServerAdminDto;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ServerAdminService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private ServerRepository serverRepository;

    public Page<ServerAdminDto> searchServers(String q,
                                              String teamId,
                                              String nodeId,
                                              GameType gameType,
                                              GameServerState state,
                                              Integer page,
                                              Integer size,
                                              String sortKey,
                                              Sort.Direction dir) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 50 : Math.max(1, size);
        Sort sort = Sort.by(dir == null ? Sort.Direction.ASC : dir, mapSortKey(sortKey));
        Pageable pageable = PageRequest.of(p, s, sort);

        Query query = new Query();
        List<Criteria> ands = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String regex = ".*" + Pattern.quote(q.trim()) + ".*";
            ands.add(new Criteria().orOperator(
                    Criteria.where("_id").regex(regex, "i"),
                    Criteria.where("displayName").regex(regex, "i")
            ));
        }
        if (teamId != null && !teamId.isBlank()) ands.add(Criteria.where("teamId").is(teamId));
        if (nodeId != null && !nodeId.isBlank()) ands.add(Criteria.where("nodeId").is(nodeId));
        if (gameType != null) ands.add(Criteria.where("gameType").is(gameType));
        if (state != null) ands.add(Criteria.where("state").is(state));
        if (!ands.isEmpty()) query.addCriteria(new Criteria().andOperator(ands));

        long total = mongoTemplate.count(query, GameServer.class);
        query.with(pageable);
        List<GameServer> servers = mongoTemplate.find(query, GameServer.class);
        return new PageImpl<>(servers.stream().map(this::toDto).toList(), pageable, total);
    }

    private String mapSortKey(String sortKey) {
        String key = (sortKey == null || sortKey.isBlank()) ? "displayName" : sortKey;
        return switch (key) {
            case "displayName" -> "displayName";
            case "created" -> "created";
            case "state" -> "state";
            case "gameType" -> "gameType";
            case "teamId" -> "teamId";
            case "nodeId" -> "nodeId";
            default -> "displayName";
        };
    }

    public ServerAdminDto get(String serverId) {
        GameServer s = serverRepository.findById(serverId).orElseThrow(NotFoundException::new);
        return toDto(s);
    }

    private ServerAdminDto toDto(GameServer s) {
        ServerAdminDto dto = new ServerAdminDto();
        dto.setId(s.getId());
        dto.setDisplayName(s.getDisplayName());
        dto.setTeamId(s.getTeamId());
        dto.setNodeId(s.getNodeId());
        dto.setCreated(s.getCreated());
        dto.setGameType(s.getGameType());
        dto.setState(s.getState());
        dto.setOnlinePlayers(s.getOnlinePlayers());
        return dto;
    }
}

