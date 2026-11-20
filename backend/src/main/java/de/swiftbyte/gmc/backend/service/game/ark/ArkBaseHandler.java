package de.swiftbyte.gmc.backend.service.game.ark;

import de.swiftbyte.gmc.backend.domain.model.SettingTemplate;
import de.swiftbyte.gmc.backend.dto.CopyProfileRequest;
import de.swiftbyte.gmc.backend.service.game.GameHandler;
import de.swiftbyte.gmc.common.model.SettingProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

public abstract class ArkBaseHandler implements GameHandler {

    @Autowired
    protected MongoTemplate mongoTemplate;

    protected SettingProfile createArkDefaultProfile(Map<String, String> variables, SettingTemplate.SettingGameType specifyingType) {
        SettingProfile profile = new SettingProfile();
        profile.setGameSettings(new LinkedHashMap<>());
        profile.setGameUserSettings(new LinkedHashMap<>());
        profile.setHyphenParams(new HashMap<>());
        profile.setQuestionMarkParams(new HashMap<>());
        profile.setGmcSettings(new HashMap<>());

        List<SettingTemplate> templates = mongoTemplate.query(SettingTemplate.class)
                .matching(query(where("").orOperator(where("gameType").is(SettingTemplate.SettingGameType.ARK_BOTH),
                        where("gameType").is(specifyingType)))
                        .addCriteria(where("defaultValue").exists(true)))
                .all();

        templates.forEach(template -> {
            switch (template.getLocation()) {
                case "GameUserSettings.ini" -> fillMap(template, profile.getGameUserSettings(), variables);
                case "Game.ini" -> fillMap(template, profile.getGameSettings(), variables);
                case "CommandLineOption" -> {
                    if (template.getSection().equals("?"))
                        profile.getQuestionMarkParams().put(template.getKey(), replaceVariables(template.getDefaultValue(), variables));
                    if (template.getSection().equals("-"))
                        profile.getHyphenParams().put(template.getKey(), replaceVariables(template.getDefaultValue(), variables));
                }
                case "GMCSettings" ->
                        profile.getGmcSettings().put(template.getKey(), replaceVariables(template.getDefaultValue(), variables));
            }
        });

        return profile;
    }

    private void fillMap(SettingTemplate template, LinkedHashMap<String, LinkedHashMap<String, Object>> map, Map<String, String> variables) {
        LinkedHashMap<String, Object> innerMap = map.computeIfAbsent(template.getSection(), k -> new LinkedHashMap<>());
        innerMap.put(template.getKey(), replaceVariables(template.getDefaultValue(), variables));
    }

    private Object replaceVariables(Object value, Map<String, String> variables) {
        if (!(value instanceof String s)) return value;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            s = s.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return s;
    }

    @Override
    public void copyInto(SettingProfile source, SettingProfile dest, CopyProfileRequest spec) {
        if (dest.getGameSettings() == null) dest.setGameSettings(new LinkedHashMap<>());
        if (dest.getGameUserSettings() == null) dest.setGameUserSettings(new LinkedHashMap<>());
        if (dest.getHyphenParams() == null) dest.setHyphenParams(new HashMap<>());
        if (dest.getQuestionMarkParams() == null) dest.setQuestionMarkParams(new HashMap<>());
        if (dest.getGmcSettings() == null) dest.setGmcSettings(new HashMap<>());

        if (spec.getGameSettingKeys() != null) {
            spec.getGameSettingKeys().forEach((section, keys) -> {
                boolean srcHasSection = source.getGameSettings() != null && source.getGameSettings().containsKey(section);
                for (String key : keys) {
                    if (srcHasSection && source.getGameSettings().get(section).containsKey(key)) {
                        Object val = source.getGameSettings().get(section).get(key);
                        dest.getGameSettings().computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, val);
                    } else {
                        if (dest.getGameSettings().containsKey(section)) {
                            dest.getGameSettings().get(section).remove(key);
                        }
                    }
                }
            });
        }

        if (spec.getGameUserSettingKeys() != null) {
            spec.getGameUserSettingKeys().forEach((section, keys) -> {
                boolean srcHasSection = source.getGameUserSettings() != null && source.getGameUserSettings().containsKey(section);
                for (String key : keys) {
                    if (srcHasSection && source.getGameUserSettings().get(section).containsKey(key)) {
                        Object val = source.getGameUserSettings().get(section).get(key);
                        dest.getGameUserSettings().computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, val);
                    } else {
                        if (dest.getGameUserSettings().containsKey(section)) {
                            dest.getGameUserSettings().get(section).remove(key);
                        }
                    }
                }
            });
        }

        if (spec.getHyphenParamKeys() != null) {
            for (String key : spec.getHyphenParamKeys()) {
                if (source.getHyphenParams() != null && source.getHyphenParams().containsKey(key)) {
                    dest.getHyphenParams().put(key, source.getHyphenParams().get(key));
                } else {
                    dest.getHyphenParams().remove(key);
                }
            }
        }

        if (spec.getQuestionMarkParamKeys() != null) {
            for (String key : spec.getQuestionMarkParamKeys()) {
                if (source.getQuestionMarkParams() != null && source.getQuestionMarkParams().containsKey(key)) {
                    dest.getQuestionMarkParams().put(key, source.getQuestionMarkParams().get(key));
                } else {
                    dest.getQuestionMarkParams().remove(key);
                }
            }
        }

        if (spec.getGmcSettingKeys() != null) {
            for (String key : spec.getGmcSettingKeys()) {
                if (source.getGmcSettings() != null && source.getGmcSettings().containsKey(key)) {
                    dest.getGmcSettings().put(key, source.getGmcSettings().get(key));
                } else {
                    dest.getGmcSettings().remove(key);
                }
            }
        }
    }

    // ---- Common helpers for ARK handlers ----

    protected Integer readServerSettingsInt(SettingProfile profile, String key) {
        if (profile == null || profile.getGameUserSettings() == null) return null;
        var section = profile.getGameUserSettings().get("ServerSettings");
        if (section == null) return null;
        Object v = section.get(key);
        if (v == null) return null;
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    protected int countCommaSeparated(Object val) {
        if (val == null) return 0;
        String s = val.toString();
        if (s.isBlank()) return 0;
        int c = 0;
        for (String p : s.split(",")) if (p != null && !p.isBlank()) c++;
        return c;
    }

    protected String readSessionSettingsString(SettingProfile profile, String key) {
        if (profile == null || profile.getGameUserSettings() == null) return null;
        var section = profile.getGameUserSettings().get("SessionSettings");
        if (section == null) return null;
        Object v = section.get(key);
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    @Override
    public String getMultihomeIp(SettingProfile profile) {
        return readSessionSettingsString(profile, "MultiHome");
    }
}
