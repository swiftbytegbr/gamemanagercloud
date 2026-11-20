package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.PageDto;
import de.swiftbyte.gmc.backend.dto.UserDto;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.UserService;
import de.swiftbyte.gmc.backend.service.admin.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/admin")
@Validated
@RequiredArgsConstructor
public class UserAdminController {

    private final PermissionService permissions;
    private final UserAdminService userAdminService;
    private final UserService userService;
    private final de.swiftbyte.gmc.backend.service.SessionService sessionService;
    private final de.swiftbyte.gmc.backend.security.SessionCookieUtil sessionCookieUtil;
    private final de.swiftbyte.gmc.backend.security.ClientIpResolver clientIpResolver;

    // ==== Users listing & search ====

    @GetMapping("/users")
    public PageDto<UserDto> listUsers(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "accountType", required = false) User.AccountType accountType,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") @Min(1) @Max(200) Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "email") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "ASC") Sort.Direction dir,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<UserDto> result = userAdminService.searchUsers(q, accountType, page, size, sort, dir);
        return PageDto.from(result);
    }

    @GetMapping("/users/{userId}")
    public UserDto getUser(@PathVariable String userId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        User user = userService.getOrThrow(userId);
        return userAdminService.toDto(user);
    }

    // ==== Safe update endpoints ====

    @Data
    public static class ChangeEmailRequest {
        @NotBlank
        private String email;
    }

    @PatchMapping("/users/{userId}/email")
    public void changeEmail(@PathVariable String userId, @RequestBody @Valid ChangeEmailRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        User user = userService.getOrThrow(userId);
        userService.changeEmail(user, request.getEmail());
    }

    @Data
    public static class ChangeAccountTypeRequest {
        @NotNull
        private User.AccountType accountType;
    }

    @PatchMapping("/users/{userId}/account-type")
    public void changeAccountType(@PathVariable String userId, @RequestBody @Valid ChangeAccountTypeRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.changeAccountType(userId, request.getAccountType());
    }

    @PatchMapping("/users/{userId}/personal-info")
    public void changePersonalInfo(@PathVariable String userId, @RequestBody @Valid User.PersonalInformation personalInformation, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.changePersonalInfo(userId, personalInformation);
    }

    @Data
    public static class UpdateCompanyInfoRequest {
        @Valid
        @NotNull
        private User.CompanyInformation companyInformation;
        @Valid
        @NotNull
        private User.PersonalInformation.Address billingAddress;
    }

    @PatchMapping("/users/{userId}/company-info")
    public void changeCompanyInfo(@PathVariable String userId, @RequestBody @Valid UpdateCompanyInfoRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.changeCompanyInfo(userId, request.getCompanyInformation(), request.getBillingAddress());
    }

    @PatchMapping("/users/{userId}/notification-settings")
    public void changeNotificationSettings(@PathVariable String userId, @RequestBody @Valid User.NotificationSettings settings, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.changeNotificationSettings(userId, settings);
    }

    // ==== Admin-only account controls ====

    @Data
    public static class EmailVerifiedRequest {
        @NotNull
        private Boolean verified;
    }

    @PatchMapping("/users/{userId}/email-verified")
    public void setEmailVerified(@PathVariable String userId, @RequestBody @Valid EmailVerifiedRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.setEmailVerified(userId, request.getVerified());
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @PatchMapping("/users/{userId}/password")
    public void changePassword(@PathVariable String userId, @RequestBody @Valid ChangePasswordRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.setPassword(userId, request.getNewPassword());
    }

    @Data
    public static class EnabledRequest {
        @NotNull
        private Boolean enabled;
    }

    @PatchMapping("/users/{userId}/enabled")
    public void setEnabled(@PathVariable String userId, @RequestBody @Valid EnabledRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.setEnabled(userId, request.getEnabled());
    }

    @Data
    public static class DeleteUserRequest {
        @NotBlank
        private String confirm;
    }

    @DeleteMapping("/users/{userId}")
    public void deleteUser(@PathVariable String userId, @RequestBody @Valid DeleteUserRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.deleteUser(userId, request.getConfirm());
    }

    @Data
    public static class UpdateMaxOwnedTeamsRequest {
        private Integer maxOwnedTeams; // null removes override
    }

    @PatchMapping("/users/{userId}/max-owned-teams")
    public void updateMaxOwnedTeams(@PathVariable String userId, @RequestBody @Valid UpdateMaxOwnedTeamsRequest request, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.updateMaxOwnedTeams(userId, request.getMaxOwnedTeams());
    }


    // ==== Sessions listing & management ====

    @GetMapping("/users/{userId}/sessions")
    public PageDto<Session> listUserSessions(
            @PathVariable String userId,
            @RequestParam(value = "includeExpired", required = false, defaultValue = "false") boolean includeExpired,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "100") @Min(1) @Max(500) Integer size,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<Session> result = userAdminService.listSessionsByUser(userId, includeExpired, page, size);
        return PageDto.from(result);
    }

    @DeleteMapping("/users/{userId}/sessions")
    public void deleteAllUserSessions(@PathVariable String userId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.deleteAllSessionsForUser(userId);
    }

    @GetMapping("/sessions")
    public PageDto<Session> listSessions(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "includeExpired", required = false, defaultValue = "false") boolean includeExpired,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "100") @Min(1) @Max(500) Integer size,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<Session> result = userAdminService.searchSessions(userId, email, q, includeExpired, page, size);
        return PageDto.from(result);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable String sessionId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        userAdminService.deleteSession(sessionId);
    }

    @Data
    public static class ImpersonateRequest {
        // Optional duration override
        @Min(1)
        @Max(4320) // up to 3 days in minutes
        private Integer durationMinutes;
    }

    @PostMapping("/users/{userId}/impersonate")
    public void impersonateUser(@PathVariable String userId,
                                @RequestBody(required = false) ImpersonateRequest body,
                                @AuthenticationPrincipal User currentUser,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        permissions.requireGmcAdmin(currentUser);
        User target = userService.getOrThrow(userId);

        int minutes = (body != null && body.getDurationMinutes() != null) ? Math.max(1, body.getDurationMinutes()) : 60;

        Session session = new Session();
        session.setUserId(target.getId());
        session.setCreated(java.time.Instant.now());
        session.setLastAccessed(java.time.Instant.now());
        session.setLastIp(clientIpResolver.resolve(request));
        session.setLastUserAgent(request.getHeader("User-Agent"));
        session.setExpiresAt(java.util.Date.from(java.time.Instant.now().plusSeconds(minutes * 60L)));
        sessionService.save(session);

        long maxAgeSeconds = Math.max(0, (session.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000);
        sessionCookieUtil.setSessionCookie(response, session.getSessionId(), maxAgeSeconds);
    }
}
