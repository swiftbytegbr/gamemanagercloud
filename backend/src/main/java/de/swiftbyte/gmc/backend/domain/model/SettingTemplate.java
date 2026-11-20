package de.swiftbyte.gmc.backend.domain.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "setting_templates")
@Accessors(chain = true)
public class SettingTemplate {

    @Id
    private String id;

    private SettingGameType gameType;

    private String location;

    private String section;

    private String key;

    private String category;

    private String displayName;

    private String description;

    private String placeholder;

    private Type type;

    private Object defaultValue;

    private boolean required;

    private String verifyingRegex;

    private String regexMessage;

    private int rangeFrom;

    private int rangeTo;

    private boolean disableSync;

    // Position refers to position in category. Starts at 0.
    private int position;

    private boolean inline;

    private List<String> dropdownValues;

    private boolean dropdownAllowCustom;

    private Appearance appearance;

    public enum SettingGameType {
        ARK_EVOLVED,
        ARK_ASCENDED,
        ARK_BOTH;
    }

    public enum Type {
        STRING,
        INTEGER,
        FLOAT,
        BOOLEAN,
        LIST;
    }

    public enum Appearance {
        TEXT,
        TEXT_AREA,
        PASSWORD,
        CHECKBOX,
        DROPDOWN,
        LIST,
        SUBTITLE;
    }
}
