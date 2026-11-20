package de.swiftbyte.gmc.backend.service.game;

import de.swiftbyte.gmc.backend.dto.CopyProfileRequest;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.model.SettingProfile;

import java.util.Map;

public interface GameHandler {
    GameType getType();

    // Create a default SettingProfile for this game type. Implementations may ignore unused variables.
    SettingProfile createDefaultProfile(Map<String, String> variables);

    // Extract the maximum number of players from the profile. Return -1 if unknown or unsupported.
    int getMaxPlayers(SettingProfile profile);

    // Number of mods configured for the server
    default int getModCount(SettingProfile profile) { return 0; }

    // Extract ports from profile; return null if not applicable/unknown
    default Integer getServerPort(SettingProfile profile) { return null; }
    default Integer getQueryPort(SettingProfile profile) { return null; }
    default Integer getRconPort(SettingProfile profile) { return null; }

    // Extract multihome/bind IP from profile if configured; return null/blank if not set
    default String getMultihomeIp(SettingProfile profile) { return null; }

    // Copy settings from source into dest based on the provided request. Default no-op for games that don't support selective copy yet.
    default void copyInto(SettingProfile source, SettingProfile dest, CopyProfileRequest spec) {
        // no-op
    }
}
