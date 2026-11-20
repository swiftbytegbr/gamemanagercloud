package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamActionLogItemPacket extends Packet {

    private ActionLogItem item;
}
