package de.swiftbyte.gmc.backend.infrastructure.packet;

import de.swiftbyte.gmc.common.packet.Packet;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Broadcast to team members when a setting profile is updated.
 * Carries the updated profile id and the user id of the issuer.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SettingProfileUpdatePacket extends Packet {

    private String profileId;
    private String userId;
}

