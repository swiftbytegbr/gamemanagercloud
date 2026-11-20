package de.swiftbyte.gmc.backend.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class CopyProfileRequest {
    private String sourceId;
    private List<String> destinationIds;
    private HashMap<String, List<String>> gameSettingKeys;
    private HashMap<String, List<String>> gameUserSettingKeys;
    private List<String> hyphenParamKeys;
    private List<String> questionMarkParamKeys;
    private List<String> gmcSettingKeys;
}

