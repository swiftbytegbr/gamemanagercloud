package de.swiftbyte.gmc.backend.domain.model;


import de.knightsoftnet.validators.shared.VatId;
import de.swiftbyte.gmc.backend.validation.Base64Image;
import de.swiftbyte.gmc.backend.validation.IsoCountryCode;
import de.swiftbyte.gmc.backend.validation.OptionalSafeName;
import de.swiftbyte.gmc.backend.validation.SafeName;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "users")
public class User {

    // -- Account Information --

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String passwordResetCode;

    private boolean emailVerified;

    private int emailVerificationCode;

    private TwoFAType twoFAType;

    private int twoFACode;

    // -- 2FA (TOTP) Settings --
    // Secret used for TOTP-based 2FA (otpauth apps). Base32-encoded.
    private String totpSecret;

    // -- 2FA Backup Codes --
    // One-time recovery codes, stored as password hashes. Never exposed in DTOs.
    private List<String> backupCodeHashes;
    // Optional: when backup codes were generated
    private Instant backupCodesCreatedAt;

    private AccountType accountType;


    private PersonalInformation personalInformation;

    // Reference to stored avatar image document (moved out of PersonalInformation)
    private String avatarImageId;

    @Valid
    private CompanyInformation companyInformation;

    private NotificationSettings notificationSettings;

    // --- Monetization/Limit Fields (per user) ---
    // If null, TeamService will fall back to YAML defaults.
    private Integer maxOwnedTeams;

    // --- Activity Tracking ---
    // Timestamp of the user's last successful login
    private Instant lastLogin;

    // --- Admin Controls ---
    // When false, login is rejected and sessions should be revoked by admin action.
    // Nullable for backward compatibility with existing documents (null => enabled).
    private Boolean enabled;


    public enum AccountType {
        USER,
        ADMIN
    }

    public enum TwoFAType {
        NONE,
        EMAIL,
        TOTP
    }


    @Data
    public static class PersonalInformation {
        @SafeName
        private String name;

        @OptionalSafeName
        private String username;

        @Base64Image(maxMegabytes = 5)
        private String avatarBase64;

        // Legacy field avatarImageId moved to User root

        @Valid
        private Address billingAddress;


        @Data
        public static class Address {
            @NotBlank(message = "invalid_street")
            @Size(max = 128, message = "invalid_street")
            private String street;

            @NotBlank(message = "invalid_city")
            @Size(max = 64, message = "invalid_city")
            private String city;

            @Size(max = 64, message = "invalid_region")
            private String region;

            @NotBlank(message = "invalid_postal_code")
            @Size(max = 32, message = "invalid_postal_code")
            private String postalCode;

            @NotBlank(message = "invalid_country_code")
            @IsoCountryCode
            private String countryCode;

            public boolean isSet() {
                return street != null || city != null || region != null || postalCode != null || countryCode != null;
            }
        }

    }

    @Data
    @VatId(fieldCountryCode = "countryCode", fieldVatId = "vatNumber", allowLowerCaseCountryCode = false, message = "invalid_vat_id")
    public static class CompanyInformation {
        @SafeName
        private String companyName;

        @IsoCountryCode
        private String countryCode;

        @NotBlank
        private String vatNumber;
    }


    @Data
    @Accessors(chain = true)
    public static class NotificationSettings {
        private boolean emailOnLogin;
        // Whether to forward GMC team notifications to the user's email
        private boolean emailNotificationsEnabled;
        // Minimum severity of notifications that should be forwarded via email
        private NotificationSeverity emailNotificationsMinSeverity;
    }


}
