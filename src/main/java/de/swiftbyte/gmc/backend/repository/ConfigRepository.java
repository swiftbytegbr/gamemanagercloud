package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.config.GmcConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConfigRepository extends MongoRepository<GmcConfig, String> {
}
