package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.common.entity.GameType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServerRepository extends MongoRepository<GameServer, String> {

    List<GameServer> findByNodeId(String nodeId);

    List<GameServer> findByNodeIdIn(List<String> nodeIds);

    Page<GameServer> findByNodeId(String nodeId, Pageable pageable);

    List<GameServer> findByGameType(GameType type);

    GameServer findBySettingProfileId(String settingProfileId);

    // Team-scoped helpers (avoid team-wide scans)
    long countByTeamId(String teamId);
    List<GameServer> findByTeamId(String teamId);
    Page<GameServer> findByTeamId(String teamId, Pageable pageable);
    long deleteByTeamId(String teamId);
    boolean existsByTeamIdAndDisplayName(String teamId, String displayName);
    boolean existsByTeamIdAndDisplayNameAndIdNot(String teamId, String displayName, String id);

    long countByIdInAndTeamId(List<String> ids, String teamId);

    // Migration helper to backfill teamId on legacy servers
    List<GameServer> findByTeamIdIsNull();


}
