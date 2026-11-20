package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.Team;
import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@Data
public class TeamDto {

    private String id;

    private String name;

    private String iconBase64;

    private String iconImageId;

    private TeamDtoMember owner;

    private List<TeamDtoMember> members;

    // Detailed invited members info (renamed to invitedMembers)
    private List<TeamDtoInvite> invitedMembers;

    private String inviteCode;

    // Effective limits for this team (resolved per-team with defaults)
    private Team.Limits limits;

    private Team.NotificationSettings notificationSettings;

    // Action log moved to dedicated controller with pagination
    @Data
    public static class TeamDtoMember {
        private String id;
        private String name;
        private String email;
        private String iconBase64;
        // New: image id reference for member avatar
        private String imageId;
        private List<Permission> permissions;
        // Indicates if the user has any form of 2FA enabled (e.g., TOTP or Email)
        private boolean twoFAEnabled;
    }

    @Data
    public static class TeamDtoInvite {
        private String id;
        private String name;
        private String email;
        private String iconBase64;
        // New: image id reference for invitee avatar
        private String imageId;
        private Instant invitedAt;
        private boolean expired;
    }


}
