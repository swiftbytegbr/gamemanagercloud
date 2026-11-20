package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.UserException;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.util.Generators;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailSender emailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ImageService imageService;


    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    public User getOrThrow(String id) {
        Optional<User> optional = findById(id);
        if (optional.isEmpty()) throw new NotFoundException();
        return optional.get();
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim());
    }


    public void save(User user) {
        userRepository.save(user);
    }

    public void delete(User user) {
        userRepository.delete(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }


    public void changeEmail(User user, String email) {
        if (existsByEmail(email)) throw new UserException("email_already_exists");
        user.setEmail(email.toLowerCase().trim());
        user.setEmailVerified(false);
        user.setEmailVerificationCode(Generators.generateVerificationToken());
        emailSender.sendVerificationEmail(user);
        save(user);
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword()))
            throw new AuthenticationException("password_invalid");
        user.setPassword(passwordEncoder.encode(newPassword));
        save(user);
    }

    public void change2fa(User user, User.TwoFAType twoFAType) {
        // Enforce valid transitions. TOTP must be enabled via the dedicated verify endpoint
        // after a setup step; do not allow switching to TOTP directly here.
        if (twoFAType == User.TwoFAType.TOTP) {
            throw new UserException("totp_enable_via_verify");
        }

        // Enforce verified email before enabling Email 2FA
        if (twoFAType == User.TwoFAType.EMAIL && !user.isEmailVerified()) {
            throw new UserException("email_not_verified_for_2fa");
        }

        user.setTwoFAType(twoFAType);
        // If disabling 2FA entirely, also clear any pending codes/secrets.
        if (twoFAType == User.TwoFAType.NONE) {
            user.setTwoFACode(0);
            user.setTotpSecret(null);
        }
        save(user);
    }

    public void changePersonalInfo(User user, User.PersonalInformation personalInformation) {
        if (personalInformation != null) {
            // If client still sends legacy base64, import to Image store and set top-level avatarImageId
            if (personalInformation.getAvatarBase64() != null && !personalInformation.getAvatarBase64().isBlank()) {
                String oldImageId = user.getAvatarImageId();
                String newImageId = imageService.saveBase64(personalInformation.getAvatarBase64());
                user.setAvatarImageId(newImageId);
                // Clear legacy base64 to avoid duplication
                personalInformation.setAvatarBase64(null);
                if (oldImageId != null && !oldImageId.isBlank()) {
                    imageService.deleteById(oldImageId);
                }
            }
            user.setPersonalInformation(personalInformation);
        }
        save(user);
    }

    /**
     * Update user's avatar image from raw bytes and store image id on the user.
     */
    public String updateAvatar(User user, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) return user.getAvatarImageId();
        String oldId = user.getAvatarImageId();
        String newId = imageService.saveBytes(imageBytes);
        user.setAvatarImageId(newId);
        // Remove legacy base64 if present on personal information
        if (user.getPersonalInformation() != null) {
            user.getPersonalInformation().setAvatarBase64(null);
        }
        save(user);
        if (oldId != null && !oldId.isBlank()) imageService.deleteById(oldId);
        return newId;
    }

    public void changeCompanyInfo(User user, User.CompanyInformation companyInformation, User.PersonalInformation.Address billingAddress) {
        if (billingAddress == null || !billingAddress.isSet()) {
            throw new UserException("address_required");
        }


        // Apply updates
        if (user.getPersonalInformation() == null) user.setPersonalInformation(new User.PersonalInformation());
        user.getPersonalInformation().setBillingAddress(billingAddress);
        user.setCompanyInformation(companyInformation);

        save(user);
    }

    public void changeNotificationSettings(User user, User.NotificationSettings notificationSettings) {
        if (notificationSettings != null && notificationSettings.isEmailNotificationsEnabled()
                && notificationSettings.getEmailNotificationsMinSeverity() == null) {
            // Default threshold to ERROR when enabling emails and none provided
            notificationSettings.setEmailNotificationsMinSeverity(NotificationSeverity.ERROR);
        }
        user.setNotificationSettings(notificationSettings);
        save(user);
    }

}
