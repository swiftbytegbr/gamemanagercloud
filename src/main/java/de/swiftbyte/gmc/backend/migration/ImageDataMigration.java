package de.swiftbyte.gmc.backend.migration;

import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Objects;

@Slf4j
@Configuration
public class ImageDataMigration {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ImageService imageService;

    @Bean
    public ApplicationRunner migrateBase64Images() {
        return args -> {
            int migratedUsers = 0;
            int migratedTeams = 0;

            // Users (legacy base64 -> image), migrate every base64 avatar
            List<User> users = userRepository.findAll();
            for (User u : users) {
                if (u.getPersonalInformation() == null) continue;
                String base64 = u.getPersonalInformation().getAvatarBase64();
                if (base64 != null && !base64.isBlank()) {
                    try {
                        String oldImageId = u.getAvatarImageId();
                        String newId = imageService.saveBase64(base64);
                        var reloaded = imageService.findById(newId);
                        if (reloaded.isPresent() && reloaded.get().getData() != null && reloaded.get().getData().length > 0) {
                            u.setAvatarImageId(newId);
                            // Null out legacy base64 after successful verification
                            u.getPersonalInformation().setAvatarBase64(null);
                            userRepository.save(u);
                            migratedUsers++;
                            if (oldImageId != null && !oldImageId.isBlank()) imageService.deleteById(oldImageId);
                        } else {
                            // Clean up orphan image if verification failed
                            imageService.deleteById(newId);
                            log.warn("Image migration failed to verify for user {}", u.getId());
                        }
                    } catch (Exception ex) {
                        log.warn("Image migration error for user {}: {}", u.getId(), ex.getMessage());
                    }
                }
            }

            // Teams (legacy base64 -> image), migrate every base64 icon
            List<Team> teams = teamRepository.findAll();
            for (Team t : teams) {
                String base64 = t.getIconBase64();
                if (base64 != null && !base64.isBlank()) {
                    try {
                        String oldImageId = t.getIconImageId();
                        String newId = imageService.saveBase64(base64);
                        var reloaded = imageService.findById(newId);
                        if (reloaded.isPresent() && reloaded.get().getData() != null && reloaded.get().getData().length > 0) {
                            t.setIconImageId(newId);
                            t.setIconBase64(null);
                            teamRepository.save(t);
                            migratedTeams++;
                            if (oldImageId != null && !oldImageId.isBlank()) imageService.deleteById(oldImageId);
                        } else {
                            imageService.deleteById(newId);
                            log.warn("Image migration failed to verify for team {}", t.getId());
                        }
                    } catch (Exception ex) {
                        log.warn("Image migration error for team {}: {}", t.getId(), ex.getMessage());
                    }
                }
            }

            if (migratedUsers > 0 || migratedTeams > 0) {
                log.info("Image migration completed. Users migrated: {}, Teams migrated: {}", migratedUsers, migratedTeams);
            } else {
                log.info("Image migration found no base64 images to migrate.");
            }
        };
    }
}
