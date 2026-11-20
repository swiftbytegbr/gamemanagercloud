package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AutomationDeletedPacket extends Packet {
    private String automationId;
    private String teamId;
}

