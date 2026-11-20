package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.backend.domain.model.automation.Automation;
import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AutomationUpdatedPacket extends Packet {
    private Automation automation;
}

