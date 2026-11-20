package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.PageDto;
import de.swiftbyte.gmc.backend.dto.TeamDto;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.admin.TeamAdminService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/teams")
@Validated
@RequiredArgsConstructor
public class TeamAdminController {

    private final PermissionService permissions;
    private final TeamAdminService teamAdminService;

    // === Search & retrieval ===

    @GetMapping
    public PageDto<TeamDto> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") @Min(1) @Max(200) Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "ASC") Sort.Direction dir,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<TeamDto> res = teamAdminService.searchTeams(q, page, size, sort, dir);
        return PageDto.from(res);
    }

    @GetMapping("/{teamId}")
    public TeamDto get(@PathVariable String teamId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        return teamAdminService.getTeamDto(teamId);
    }

    // === Editing ===

    @Data
    public static class ChangeNameRequest {
        @NotBlank
        private String name;
    }

    @PatchMapping("/{teamId}/name")
    public void changeName(@PathVariable String teamId, @RequestBody @Valid ChangeNameRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.changeName(teamId, req.getName());
    }

    @Data
    public static class TransferOwnerRequest {
        @NotBlank
        private String newOwnerId;
    }

    @PatchMapping("/{teamId}/owner")
    public void transferOwner(@PathVariable String teamId, @RequestBody @Valid TransferOwnerRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.transferOwnership(teamId, req.getNewOwnerId());
    }

    @PatchMapping("/{teamId}/limits")
    public void updateLimits(@PathVariable String teamId, @RequestBody @Valid Team.Limits limits, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.updateLimits(teamId, limits);
    }

    @PatchMapping("/{teamId}/notification-settings")
    public void updateNotificationSettings(@PathVariable String teamId, @RequestBody @Valid Team.NotificationSettings settings, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.updateNotificationSettings(teamId, settings);
    }

    @PostMapping(value = "/{teamId}/icon", consumes = { "multipart/form-data" })
    public void uploadIcon(@PathVariable String teamId, @RequestPart("icon") MultipartFile icon, @AuthenticationPrincipal User currentUser) throws Exception {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.changeIcon(teamId, icon.getBytes());
    }

    @Data
    public static class AddMemberRequest {
        @NotBlank
        private String userId;
        private List<Permission> permissions; // default ACCESS_TEAM
    }

    @PostMapping("/{teamId}/members")
    public void addMember(@PathVariable String teamId, @RequestBody @Valid AddMemberRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.addMember(teamId, req.getUserId(), req.getPermissions());
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public void removeMember(@PathVariable String teamId, @PathVariable String userId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.removeMember(teamId, userId);
    }

    @Data
    public static class ChangeMemberPermissionsRequest {
        @NotNull
        private List<Permission> permissions;
    }

    @PatchMapping("/{teamId}/members/{userId}/permissions")
    public void changeMemberPermissions(@PathVariable String teamId, @PathVariable String userId, @RequestBody @Valid ChangeMemberPermissionsRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.changeMemberPermissions(teamId, userId, req.getPermissions());
    }

    @Data
    public static class DeleteTeamRequest {
        @NotBlank
        private String confirm;
    }

    @DeleteMapping("/{teamId}")
    public void deleteTeam(@PathVariable String teamId, @RequestBody @Valid DeleteTeamRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        teamAdminService.deleteTeamCascade(teamId, req.getConfirm());
    }
}
