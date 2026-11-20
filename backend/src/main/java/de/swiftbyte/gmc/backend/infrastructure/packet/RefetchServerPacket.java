package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Signals the frontend to refetch a specific server by id.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RefetchServerPacket extends Packet {

    private String serverId;
}

