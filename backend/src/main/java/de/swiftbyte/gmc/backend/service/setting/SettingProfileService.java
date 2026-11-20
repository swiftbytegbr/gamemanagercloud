package de.swiftbyte.gmc.backend.service.setting;

// no direct controller imports needed here

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.swiftbyte.gmc.backend.config.UploadLimits;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.dto.CopyProfileRequest;
import de.swiftbyte.gmc.backend.exception.BadRequestException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.SettingsException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.SettingProfileRepository;
import de.swiftbyte.gmc.backend.service.game.GameHandlersRegistry;
import de.swiftbyte.gmc.backend.util.Generators;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.model.SettingProfile;
import de.swiftbyte.gmc.common.packet.from.backend.server.ServerSettingsPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class SettingProfileService {


    @Autowired
    private SettingProfileRepository settingProfileRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private StompSender stomp;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GameHandlersRegistry gameHandlers;


    // ---- Validation methods ----

    private void validateProfileSize(SettingProfile profile) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(profile);
            if (data != null && data.length > UploadLimits.MAX_FILE_SIZE_BYTES) {
                throw new SettingsException("profile_too_large");
            }
        } catch (JsonProcessingException e) {
            throw new BadRequestException("general.bad_request");
        }
    }

    // ---- CRUD methods ----

    public SettingProfile getOrThrow(String profileId) {
        return settingProfileRepository.findById(profileId).orElseThrow(NotFoundException::new);
    }

    public void save(SettingProfile profile) {
        validateProfileSize(profile);
        settingProfileRepository.save(profile);
    }

    public void updateProfile(SettingProfile profile) {
        validateProfileSize(profile);
        settingProfileRepository.save(profile);
        findAllServerIdsByProfile(profile).forEach(server -> {
            ServerSettingsPacket packet = new ServerSettingsPacket();
            packet.setServerId(server.getId());
            packet.setSettingProfileId(profile.getId());
            stomp.sendToNode(server.getNodeId(), "/server/settings", packet);
        });
    }

    public List<GameServer> findAllServerIdsByProfile(SettingProfile profile) {
        return mongoTemplate.query(GameServer.class).matching(query(where("settingProfileId").is(profile.getId()))).all();
    }

    /**
     * Sync selected categories/keys from a source profile onto the target server's profile,
     * then notify the node to apply new settings.
     */
    public void syncProfileToServer(String sourceProfileId, GameServer server, CopyProfileRequest selector) {
        SettingProfile source = getOrThrow(sourceProfileId);
        SettingProfile dest = getOrThrow(server.getSettingProfileId());

        if (!Objects.equals(source.getTeamId(), dest.getTeamId())) throw new SettingsException("teams_not_equal");
        if (!Objects.equals(source.getGameType(), dest.getGameType())) throw new SettingsException("game_types_not_equal");

        GameType type = GameType.valueOf(source.getGameType());
        var handler = gameHandlers.get(type);
        if (handler == null) throw new SettingsException("unsupported_game_type");

        handler.copyInto(source, dest, selector);
        updateProfile(dest);
    }

    // ---- business logic delegated to handlers where possible ----

    public void copyArkProfiles(CopyProfileRequest request) {
        // Validate request: source must not be among destinations
        if (request.getDestinationIds() != null && request.getDestinationIds().contains(request.getSourceId())) {
            throw new SettingsException("copy_targets_contains_source");
        }

        SettingProfile source = getOrThrow(request.getSourceId());

        // Collect and validate teamIds and gameTypes (single pass over destinations)
        Set<String> teamIds = new HashSet<>();
        Set<String> gameTypes = new HashSet<>();
        teamIds.add(source.getTeamId());
        gameTypes.add(source.getGameType());
        List<SettingProfile> destinations = new ArrayList<>();
        for (String destId : request.getDestinationIds()) {
            SettingProfile dest = getOrThrow(destId);
            destinations.add(dest);
            teamIds.add(dest.getTeamId());
            gameTypes.add(dest.getGameType());
        }
        if (teamIds.size() != 1 || teamIds.contains(null)) throw new SettingsException("teams_not_equal");
        if (gameTypes.size() != 1 || gameTypes.contains(null)) throw new SettingsException("game_types_not_equal");

        // Perform copy for each destination via game handler and notify servers
        GameType type = GameType.valueOf(source.getGameType());
        if (type != GameType.ARK_ASCENDED && type != GameType.ARK_EVOLVED) {
            throw new SettingsException("copy_not_supported");
        }
        var handler = gameHandlers.get(type);
        if (handler == null) throw new SettingsException("unsupported_game_type");

        for (SettingProfile dest : destinations) {
            handler.copyInto(source, dest, request);
            updateProfile(dest);
        }
    }

    // ---- Template backfill support ----

    // Invoked after new templates are uploaded. For each existing profile, generate the
    // default profile via the appropriate game handler and merge any missing keys only.
    // Existing values remain untouched. Servers using updated profiles get notified.
    public int backfillDefaultsForAllProfilesAfterTemplateUpload() {
        List<SettingProfile> allProfiles = settingProfileRepository.findAll();
        int updated = 0;
        for (SettingProfile profile : allProfiles) {
            if (profile == null || profile.getGameType() == null) continue;
            GameType type;
            try {
                type = GameType.valueOf(profile.getGameType());
            } catch (IllegalArgumentException ex) {
                continue; // unknown or unsupported type in this backend
            }

            var handler = gameHandlers.get(type);
            if (handler == null) continue;

            Map<String, String> variables = new HashMap<>();
            // Provide a random RCON password for defaults that require it (only used if missing keys)
            variables.put("randomRconPassword", Generators.generateRconPassword());
            SettingProfile defaults = handler.createDefaultProfile(variables);

            boolean changed = mergeMissing(profile, defaults);
            if (changed) {
                updateProfile(profile);
                updated++;
            }
        }
        return updated;
    }

    // Merge missing keys from src into dest without overwriting existing values
    private boolean mergeMissing(SettingProfile dest, SettingProfile src) {
        boolean[] changed = {false};

        if (dest.getGameSettings() == null) dest.setGameSettings(new LinkedHashMap<>());
        if (dest.getGameUserSettings() == null) dest.setGameUserSettings(new LinkedHashMap<>());
        if (dest.getHyphenParams() == null) dest.setHyphenParams(new HashMap<>());
        if (dest.getQuestionMarkParams() == null) dest.setQuestionMarkParams(new HashMap<>());
        if (dest.getGmcSettings() == null) dest.setGmcSettings(new HashMap<>());

        if (src.getGameSettings() != null) {
            src.getGameSettings().forEach((section, keys) -> {
                LinkedHashMap<String, Object> dstSection = dest.getGameSettings().computeIfAbsent(section, k -> new LinkedHashMap<>());
                keys.forEach((k, v) -> {
                    if (!dstSection.containsKey(k)) {
                        dstSection.put(k, v);
                        changed[0] = true;
                    }
                });
            });
        }

        if (src.getGameUserSettings() != null) {
            src.getGameUserSettings().forEach((section, keys) -> {
                LinkedHashMap<String, Object> dstSection = dest.getGameUserSettings().computeIfAbsent(section, k -> new LinkedHashMap<>());
                keys.forEach((k, v) -> {
                    if (!dstSection.containsKey(k)) {
                        dstSection.put(k, v);
                        changed[0] = true;
                    }
                });
            });
        }

        if (src.getHyphenParams() != null) {
            src.getHyphenParams().forEach((k, v) -> {
                if (!dest.getHyphenParams().containsKey(k)) {
                    dest.getHyphenParams().put(k, v);
                    changed[0] = true;
                }
            });
        }

        if (src.getQuestionMarkParams() != null) {
            src.getQuestionMarkParams().forEach((k, v) -> {
                if (!dest.getQuestionMarkParams().containsKey(k)) {
                    dest.getQuestionMarkParams().put(k, v);
                    changed[0] = true;
                }
            });
        }

        if (src.getGmcSettings() != null) {
            src.getGmcSettings().forEach((k, v) -> {
                if (!dest.getGmcSettings().containsKey(k)) {
                    dest.getGmcSettings().put(k, v);
                    changed[0] = true;
                }
            });
        }

        return changed[0];
    }

}
