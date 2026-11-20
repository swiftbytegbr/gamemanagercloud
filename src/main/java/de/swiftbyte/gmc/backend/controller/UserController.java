package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.UserDto;
import de.swiftbyte.gmc.backend.mapper.UserMapper;
import de.swiftbyte.gmc.backend.service.TwoFactorService;
import de.swiftbyte.gmc.backend.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    UserMapper userMapper;

    @Autowired
    TwoFactorService twoFactorService;

    @GetMapping("/@me")
    public UserDto getMe(@AuthenticationPrincipal User user) {
        return userMapper.toDto(user);
    }

    @Data
    public static class ChangeEmailRequest {
        @Email
        @NotBlank
        private String email;
    }

    @PostMapping("/@me/change-email")
    public void changeEmail(@AuthenticationPrincipal User user, @RequestBody @Valid ChangeEmailRequest request) {
        userService.changeEmail(user, request.getEmail());
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        @Size(min = 8, max = 256)
        private String oldPassword;
        @NotBlank
        @Size(min = 8, max = 256)
        private String newPassword;
    }

    @PostMapping("/@me/change-password")
    public void changePassword(@AuthenticationPrincipal User user, @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(user, request.getOldPassword(), request.getNewPassword());
    }

    // Note: 2FA management endpoints moved to dedicated controllers for TOTP and Email.

    @Data
    public static class DeleteAccountRequest {
        @AssertTrue
        private boolean confirmation;
    }

    @DeleteMapping("/@me")
    public void deleteAccount(@RequestBody @Valid DeleteAccountRequest request, @AuthenticationPrincipal User user) {
        userService.delete(user);
    }

    @Data
    @Valid
    public static class UpdatePersonalInfoRequest {
        @Valid
        private User.PersonalInformation personalInformation;
    }

    @PostMapping("/@me/personal-info")
    public void changePersonalInfo(@AuthenticationPrincipal User user, @RequestBody @Valid UpdatePersonalInfoRequest request) {
        userService.changePersonalInfo(user, request.getPersonalInformation());
    }

    @Data
    public static class UploadAvatarResponse {
        private String imageId;
    }

    @PostMapping(value = "/@me/avatar", consumes = {"image/*"})
    public UploadAvatarResponse uploadAvatar(@AuthenticationPrincipal User user, @RequestBody byte[] image) {
        String newId = userService.updateAvatar(user, image);
        UploadAvatarResponse res = new UploadAvatarResponse();
        res.setImageId(newId);
        return res;
    }

    @Data
    public static class UpdateCompanyInfoRequest {
        @Valid
        @NotNull(message = "company_info_required")
        private User.CompanyInformation companyInformation;
        @Valid
        @NotNull(message = "address_required")
        private User.PersonalInformation.Address billingAddress;
    }

    @PostMapping("/@me/company-info")
    public void changeCompanyInfo(@AuthenticationPrincipal User user, @RequestBody @Valid UpdateCompanyInfoRequest request) {
        userService.changeCompanyInfo(user, request.getCompanyInformation(), request.getBillingAddress());
    }

    @PostMapping("/@me/notification-settings")
    public void changeNotificationSettings(@AuthenticationPrincipal User user, @RequestBody @Valid User.NotificationSettings notificationSettings) {
        userService.changeNotificationSettings(user, notificationSettings);
    }

}
