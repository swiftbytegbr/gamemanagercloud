package de.swiftbyte.gmc.backend.domain.value.automation;

import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

import java.util.ArrayList;
import java.util.List;

@Data
@TypeAlias("auto.targets.explicit")
public class ExplicitTargetSelector extends TargetSelector {
    private List<String> ids = new ArrayList<>();
}

