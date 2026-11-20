package de.swiftbyte.gmc.backend.service.game.ark;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.model.SettingProfile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ArkEvolvedHandler extends ArkBaseHandler {

    @Override
    public GameType getType() {
        return GameType.ARK_EVOLVED;
    }

    @Override
    public SettingProfile createDefaultProfile(Map<String, String> variables) {
        SettingProfile profile = createArkDefaultProfile(variables, SettingTemplate.SettingGameType.ARK_EVOLVED);
        profile.setGameType(GameType.ARK_EVOLVED.toString());
        return profile;
    }

    @Override
    public int getMaxPlayers(SettingProfile profile) {
        try {
            Object value = profile.getGameUserSettings()
                    .get("/Script/Engine.GameSession")
                    .get("MaxPlayers");
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int getModCount(SettingProfile profile) {
        try {
            var section = profile.getGameUserSettings() == null ? null : profile.getGameUserSettings().get("ServerSettings");
            if (section != null) {
                Object v = section.get("ActiveMods");
                if (v != null) {
                    String s = v.toString();
                    if (!s.isBlank()) {
                        int c = 0;
                        for (String p : s.split(",")) if (p != null && !p.isBlank()) c++;
                        return c;
                    }
                }
            }
        } catch (Exception ignored) { }
        return 0;
    }

    @Override
    public Integer getServerPort(SettingProfile profile) {
        // ASE: key is capitalized "Port"
        try {
            Object v = profile.getQuestionMarkParams().get("Port");
            return v == null ? null : Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }

    @Override
    public Integer getQueryPort(SettingProfile profile) {
        try {
            Object v = profile.getQuestionMarkParams().get("QueryPort");
            return v == null ? null : Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }

    @Override
    public Integer getRconPort(SettingProfile profile) {
        try {
            var section = profile.getGameUserSettings() == null ? null : profile.getGameUserSettings().get("ServerSettings");
            if (section == null) return null;
            Object v = section.get("RCONPort");
            return v == null ? null : Integer.parseInt(v.toString());
        } catch (Exception e) { return null; }
    }
}
