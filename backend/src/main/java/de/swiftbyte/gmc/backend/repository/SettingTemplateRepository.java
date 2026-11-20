package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SettingTemplateRepository extends MongoRepository<SettingTemplate, String> {

    SettingTemplate findByLocationAndSectionAndKey(String location, String section, String key);

    List<SettingTemplate> getAllByGameType(SettingTemplate.SettingGameType gameType);

    List<SettingTemplate> getAllByGameTypeOrGameType(SettingTemplate.SettingGameType gameType, SettingTemplate.SettingGameType arkBoth);

}
