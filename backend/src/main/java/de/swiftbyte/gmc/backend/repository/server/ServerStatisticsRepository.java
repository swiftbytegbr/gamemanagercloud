package de.swiftbyte.gmc.backend.repository.server;

import de.swiftbyte.gmc.backend.domain.model.server.GameServerStatistic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServerStatisticsRepository extends MongoRepository<GameServerStatistic, String> {
    long deleteByServerIdIn(java.util.List<String> serverIds);
}
