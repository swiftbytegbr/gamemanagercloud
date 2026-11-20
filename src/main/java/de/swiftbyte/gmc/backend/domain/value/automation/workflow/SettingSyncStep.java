package de.swiftbyte.gmc.backend.domain.value.automation.workflow;

import de.swiftbyte.gmc.backend.domain.enums.automation.AutomationStepType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

import java.util.HashMap;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TypeAlias("auto.step.settingSync")
public class SettingSyncStep extends AutomationStep {
    private String profileId;
    private HashMap<String, List<String>> gameSettingKeys;
    private HashMap<String, List<String>> gameUserSettingKeys;
    private List<String> hyphenParamKeys;
    private List<String> questionMarkParamKeys;
    private List<String> gmcSettingKeys;
    public SettingSyncStep() { setType(AutomationStepType.SETTING_SYNC); }
}

