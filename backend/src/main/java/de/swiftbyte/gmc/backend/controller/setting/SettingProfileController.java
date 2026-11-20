package de.swiftbyte.gmc.backend.controller.setting;

import de.swiftbyte.gmc.backend.config.UploadLimits;
import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.CopyProfileRequest;
import de.swiftbyte.gmc.backend.events.settings.SettingProfileUpdatedEvent;
import de.swiftbyte.gmc.backend.exception.SettingsException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.SettingProfileUpdatePacket;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.SettingProfileRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.setting.IniService;
import de.swiftbyte.gmc.backend.service.setting.SettingProfileService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import de.swiftbyte.gmc.common.model.SettingProfile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;

@RestController
@RequestMapping("/setting/profile")
@Validated
@ApiAccountAllowed
public class SettingProfileController {

    // Use shared 5MB limit for INI payloads as well
    private static final int MAX_INI_FILE_SIZE = UploadLimits.MAX_FILE_SIZE_BYTES;


    @Autowired
    private SettingProfileService profileService;

    @Autowired
    private SettingProfileRepository profileRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private IniService iniService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private StompSender stompSender;

    @Autowired
    private TeamRepository teamRepository;




    @GetMapping("/{profileId}")
    public SettingProfile getProfile(@PathVariable @Identifier String profileId, Authentication authentication, @AuthenticationPrincipal User user) {
        SettingProfile profile = profileService.getOrThrow(profileId);
        permissionService.requireByTeamIdAndAllowNodeAccess(user, profile.getTeamId(), Permission.ACCESS_PROFILES, authentication);
        return profile;
    }

    @GetMapping("/{profileId}/game.ini")
    public String getGameIni(@PathVariable @Identifier String profileId, Authentication authentication, @AuthenticationPrincipal User user) {
        SettingProfile profile = profileService.getOrThrow(profileId);
        permissionService.requireByTeamIdAndAllowNodeAccess(user, profile.getTeamId(), Permission.ACCESS_PROFILES, authentication);
        return iniService.generateIni(profile.getGameSettings());
    }

    @GetMapping("/{profileId}/gameUserSettings.ini")
    public String getGameUserSettingsIni(@PathVariable @Identifier String profileId, Authentication authentication, @AuthenticationPrincipal User user) {
        SettingProfile profile = profileService.getOrThrow(profileId);
        permissionService.requireByTeamIdAndAllowNodeAccess(user, profile.getTeamId(), Permission.ACCESS_PROFILES, authentication);
        return iniService.generateIni(profile.getGameUserSettings());
    }

    // Inline DTO as requested, placed above the controller method
    @Data
    public static class SettingProfileUpdateRequest {
        private String id;
        private String teamId;
        private String map;
        private LinkedHashMap<String, LinkedHashMap<String, Object>> gameSettings;
        private LinkedHashMap<String, LinkedHashMap<String, Object>> gameUserSettings;
        private HashMap<String, Object> questionMarkParams;
        private HashMap<String, Object> hyphenParams;
        private HashMap<String, Object> gmcSettings;
    }

    @PostMapping()
    public void updateProfile(@RequestBody SettingProfileUpdateRequest request, @AuthenticationPrincipal User user) {
        SettingProfile existingProfile = profileService.getOrThrow(request.getId());
        if (request.getTeamId() != null && !existingProfile.getTeamId().equals(request.getTeamId()))
            throw new SettingsException("teams_not_equal");
        // Permissions are checked against the existing profile's team
        permissionService.requireByTeamId(user, existingProfile.getTeamId(), Permission.MANAGE_PROFILES);

        // Merge only provided fields into the existing profile
        if (request.getMap() != null) existingProfile.setMap(request.getMap());

        if (request.getGameSettings() != null) existingProfile.setGameSettings(request.getGameSettings());
        if (request.getGameUserSettings() != null) existingProfile.setGameUserSettings(request.getGameUserSettings());
        if (request.getQuestionMarkParams() != null) existingProfile.setQuestionMarkParams(request.getQuestionMarkParams());
        if (request.getHyphenParams() != null) existingProfile.setHyphenParams(request.getHyphenParams());
        if (request.getGmcSettings() != null) existingProfile.setGmcSettings(request.getGmcSettings());

        profileService.updateProfile(existingProfile);

        var server = serverRepository.findBySettingProfileId(existingProfile.getId());
        eventPublisher.publishEvent(new SettingProfileUpdatedEvent(user.getId(), server));

        // Notify all team members about the profile update so UIs can warn about overwrites
        var team = teamRepository.findById(existingProfile.getTeamId()).orElse(null);
        if (team != null) {
            SettingProfileUpdatePacket packet = new SettingProfileUpdatePacket();
            packet.setProfileId(existingProfile.getId());
            packet.setUserId(user.getId());
            // Exclude the acting user from receiving their own update
            stompSender.sendToTeam(team, "/setting/profile-update", packet, Collections.singleton(user.getId()));
        }

    }

    @PostMapping("/parse-ini")
    public LinkedHashMap<String, LinkedHashMap<String, Object>> parseIni(@RequestBody @Valid FileBody ini, @AuthenticationPrincipal User user) {
        // Validate body and size to avoid NPEs and excessive payloads
        if (ini == null || ini.file == null || ini.file.isBlank()) {
            throw new SettingsException("ini_empty");
        }
        if (ini.file.length() > MAX_INI_FILE_SIZE) {
            throw new SettingsException("ini_too_large");
        }
        return iniService.parseIni(ini.file);
    }

    @PostMapping("/generate-ini")
    public String generateIni(@RequestBody LinkedHashMap<String, LinkedHashMap<String, Object>> ini, @AuthenticationPrincipal User user) {
        return iniService.generateIni(ini);
    }

    @Data
    public static class FileBody {
        @NotBlank
        @Size(max = MAX_INI_FILE_SIZE)
        String file;
    }


    @PostMapping("/copy")
    public void copyArkProfiles(@RequestBody CopyProfileRequest request, @AuthenticationPrincipal User user) {
        SettingProfile sourceProfile = profileService.getOrThrow(request.getSourceId());
        permissionService.requireByTeamId(user, sourceProfile.getTeamId(), Permission.MANAGE_PROFILES);
        profileService.copyArkProfiles(request);
    }
}
