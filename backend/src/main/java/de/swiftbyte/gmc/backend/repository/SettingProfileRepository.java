package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.common.model.SettingProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SettingProfileRepository extends MongoRepository<SettingProfile, String> {

    long deleteByTeamId(String teamId);
}
