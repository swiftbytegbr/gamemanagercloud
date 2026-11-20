package de.swiftbyte.gmc.backend.dto;

import de.swiftbyte.gmc.backend.domain.model.User;
import lombok.Data;

@Data
public class UserDto {

    // -- Account Information --

    private String id;

    private String email;

    private boolean emailVerified;

    private User.TwoFAType twoFAType;

    private User.AccountType accountType;

    private User.PersonalInformation personalInformation;

    private String avatarImageId;

    private User.CompanyInformation companyInformation;

    private User.NotificationSettings notificationSettings;

    // Optional: per-user limit for owned teams (may be null)
    private Integer maxOwnedTeams;

    // Admin controls
    private Boolean enabled;

}
