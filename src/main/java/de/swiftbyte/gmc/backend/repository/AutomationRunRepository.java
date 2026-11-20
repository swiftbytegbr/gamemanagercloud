package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.automation.AutomationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AutomationRunRepository extends MongoRepository<AutomationRun, String> {
    Page<AutomationRun> findByTeamId(String teamId, Pageable pageable);
    Page<AutomationRun> findByTeamIdAndAutomationId(String teamId, String automationId, Pageable pageable);
    long deleteByTeamId(String teamId);
}
