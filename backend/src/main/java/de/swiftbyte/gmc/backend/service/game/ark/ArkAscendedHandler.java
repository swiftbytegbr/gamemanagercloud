package de.swiftbyte.gmc.backend.service.game.ark;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.model.SettingProfile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ArkAscendedHandler extends ArkBaseHandler {

    @Override
    public GameType getType() {
        return GameType.ARK_ASCENDED;
    }

    @Override
    public SettingProfile createDefaultProfile(Map<String, String> variables) {
        SettingProfile profile = createArkDefaultProfile(variables, SettingTemplate.SettingGameType.ARK_ASCENDED);
        profile.setGameType(GameType.ARK_ASCENDED.toString());
        return profile;
    }

    @Override
    public int getMaxPlayers(SettingProfile profile) {
        try {
            Object value = profile.getHyphenParams().get("WinLiveMaxPlayers");
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int getModCount(SettingProfile profile) {
        // ASA commonly uses CLI ?mods or -mods, but also may support ActiveMods
        try {
            String mods = null;
            if (profile.getQuestionMarkParams() != null && profile.getQuestionMarkParams().get("mods") != null) {
                mods = String.valueOf(profile.getQuestionMarkParams().get("mods"));
            }
            if ((mods == null || mods.isBlank()) && profile.getHyphenParams() != null && profile.getHyphenParams().get("mods") != null) {
                mods = String.valueOf(profile.getHyphenParams().get("mods"));
            }
            if (mods != null && !mods.isBlank()) {
                return countCommaSeparated(mods);
            }
            var section = profile.getGameUserSettings() == null ? null : profile.getGameUserSettings().get("ServerSettings");
            if (section != null) {
                Object v = section.get("ActiveMods");
                if (v != null) return countCommaSeparated(v);
            }
        } catch (Exception ignored) { }
        return 0;
    }

    @Override
    public Integer getServerPort(SettingProfile profile) {
        try {
            Object v = profile.getHyphenParams().get("port");
            return v == null ? null : Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }

    @Override
    public Integer getQueryPort(SettingProfile profile) {
        // No query port on ASA via this key set; return null
        return null;
    }

    @Override
    public Integer getRconPort(SettingProfile profile) {
        return readServerSettingsInt(profile, "RCONPort");
    }
}
