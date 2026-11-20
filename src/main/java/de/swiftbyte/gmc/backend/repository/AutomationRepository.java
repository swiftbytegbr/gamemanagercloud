package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AutomationRepository extends MongoRepository<Automation, String> {
    List<Automation> findByTeamIdOrderByUpdatedAtDesc(String teamId);
    long deleteByTeamId(String teamId);
}
