package de.swiftbyte.gmc.backend.domain.value.automation;

import de.swiftbyte.gmc.backend.domain.enums.automation.TargetSelectorOp;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;

@Data
@TypeAlias("auto.targets.dynamic")
public class DynamicTargetSelector extends TargetSelector {
    /**
     * Dynamic filters (AND semantics)
     */
    private List<Entry> selectors = new ArrayList<>();

    @Data
    public static class Entry {
        private Field field;
        private TargetSelectorOp op = TargetSelectorOp.EQ;
        private String value;
    }

    public enum Field {
        NODE_ID,
        GAME_TYPE,
        STATE,
        DISPLAY_NAME
    }
}
