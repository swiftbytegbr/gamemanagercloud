package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamActionLogRepository extends MongoRepository<ActionLogItem, String> {

    Page<ActionLogItem> findByTeamId(String teamId, Pageable pageable);

    long countByTeamId(String teamId);

    long deleteByTeamId(String teamId);
}
