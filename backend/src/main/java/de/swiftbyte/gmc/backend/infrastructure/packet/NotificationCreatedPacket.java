package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.backend.dto.NotificationDto;
import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationCreatedPacket extends Packet {
    private NotificationDto notification;
}

