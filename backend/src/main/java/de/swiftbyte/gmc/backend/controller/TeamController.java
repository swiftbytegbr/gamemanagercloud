package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.ApplicationDto;
import de.swiftbyte.gmc.backend.dto.TeamDto;
import de.swiftbyte.gmc.backend.events.team.*;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.TeamService;
import de.swiftbyte.gmc.backend.validation.DiscordWebhookUrl;
import de.swiftbyte.gmc.backend.validation.Identifier;
import de.swiftbyte.gmc.backend.validation.SafeName;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/team")
@Validated
public class TeamController {

    @Autowired
    private TeamService teamService;
    @Autowired
    private PermissionService permissions;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private UserRepository userRepository;

    @Data
    public static class CreateTeamRequest {
        @SafeName
        private String name;
    }

    @PostMapping("/create")
    public Team createTeam(@RequestBody @Valid CreateTeamRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.createTeam(request.getName(), user);

        eventPublisher.publishEvent(new TeamCreatedEvent(user.getId(), team));
        return team;
    }

    @Data
    public static class JoinTeamRequest {
        @NotBlank
        @Size(min = 4, max = 64)
        private String inviteCode;
    }

    @PostMapping("/join")
    public void joinTeam(@RequestBody @Valid JoinTeamRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.joinTeam(request.getInviteCode(), user);
        eventPublisher.publishEvent(new TeamJoinedEvent(user.getId(), team, user.getId()));
    }

    @GetMapping("/my")
    @ApiAccountAllowed
    public List<TeamDto> getMyTeams(@AuthenticationPrincipal User user) {
        return teamService.getTeamDtosByUserIncludingApplications(user);
    }

    @GetMapping("/{teamId}")
    @ApiAccountAllowed
    public TeamDto getTeam(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_TEAM);
        return teamService.getTeamDtoOrThrow(teamId);
    }

    @DeleteMapping("/{teamId}")
    public void deleteTeam(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.requireOwner(user, team);
        teamService.deleteTeam(team);
    }

    @PostMapping("/{teamId}/leave")
    public void leaveTeam(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        teamService.leaveTeam(user, team);
        eventPublisher.publishEvent(new TeamLeftEvent(user.getId(), team, user.getId()));
    }

    @Data
    public static class KickMemberRequest {
        @Identifier
        private String memberId;
    }

    @ApiAccountAllowed
    @PostMapping("/{teamId}/kick")
    public void kickMember(@PathVariable @Identifier String teamId, @RequestBody @Valid KickMemberRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        teamService.kickMember(team, request.getMemberId());
        eventPublisher.publishEvent(new TeamKickedEvent(user.getId(), team, request.getMemberId()));
    }

    @Data
    public static class InviteMemberRequest {
        @Email
        @NotBlank
        private String userEmail;
    }

    @ApiAccountAllowed
    @PostMapping("/{teamId}/invite")
    public void inviteMember(@PathVariable @Identifier String teamId, @RequestBody @Valid InviteMemberRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        teamService.inviteMember(team, request.getUserEmail());
        eventPublisher.publishEvent(new TeamInvitationCreatedEvent(user.getId(), team, request.userEmail));
    }

    @ApiAccountAllowed
    @DeleteMapping("/{teamId}/invite/{userEmail}")
    public void revokeInvite(@PathVariable @Identifier String teamId, @PathVariable @Identifier String userEmail, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        teamService.revokeInvite(team, userEmail);
        eventPublisher.publishEvent(new TeamInvitationRevokedEvent(user.getId(), team, userEmail));
    }

    @Data
    public static class ChangeMemberPermissionRequest {
        @Identifier
        private String memberId;
        @NotNull
        private List<Permission> newPermissions;
    }

    @ApiAccountAllowed
    @PostMapping("/{teamId}/permission")
    public void changeMemberPermission(@PathVariable @Identifier String teamId, @RequestBody @Valid ChangeMemberPermissionRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        this.permissions.require(user, team, Permission.MANAGE_TEAM);

        // Check if the user is trying to change their own permissions and return forbidden exception with error key
        if (user.getId().equals(request.getMemberId())) {
            throw new TeamException("cannot_change_own_permissions");
        }
        List<Permission> oldPermissions = team.getOwnerId().equals(request.getMemberId())
                ? List.of(Permission.values())
                : team.getMembers().getOrDefault(request.getMemberId(), List.of());

        User member = userRepository.findById(request.getMemberId()).orElseThrow(NotFoundException::new);

        teamService.changeMemberPermission(team, member, request.getNewPermissions());
        eventPublisher.publishEvent(new TeamPermissionChangedEvent(
                user.getId(), team, request.getMemberId(), oldPermissions, request.getNewPermissions()
        ));
    }

    @Data
    public static class ChangeTeamNameRequest {
        @SafeName
        private String name;
    }

    @ApiAccountAllowed
    @PostMapping("/{teamId}/name")
    public void changeTeamName(@PathVariable @Identifier String teamId, @RequestBody @Valid ChangeTeamNameRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        String oldName = team.getName();
        teamService.changeTeamName(team, request.getName());
        eventPublisher.publishEvent(new TeamNameChangedEvent(user.getId(), team, oldName, request.getName()));
    }

    @ApiAccountAllowed
    @PostMapping(value = "/{teamId}/icon", consumes = {"image/*"})
    public void changeTeamIcon(@PathVariable @Identifier String teamId, @RequestBody byte[] iconBytes, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);

        teamService.changeTeamIconFromBytes(team, iconBytes);
        eventPublisher.publishEvent(new TeamIconChangedEvent(user.getId(), team));
    }

    // === Team Notification Settings (Discord) ===

    @Data
    public static class NotificationSettingsRequest {
        @DiscordWebhookUrl
        private String discordWebhookUrl; // null/blank disables
        private NotificationSeverity discordNotificationsMinSeverity; // null defaults to ERROR
    }

    @Data
    public static class NotificationSettingsResponse {
        private String discordWebhookUrl;
        private NotificationSeverity discordNotificationsMinSeverity;
    }

    @GetMapping("/{teamId}/notification-settings")
    public NotificationSettingsResponse getNotificationSettings(
            @PathVariable @Identifier String teamId,
            @AuthenticationPrincipal User user
    ) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        NotificationSettingsResponse resp = new NotificationSettingsResponse();
        Team.NotificationSettings s = team.getNotificationSettings();
        if (s != null) {
            resp.setDiscordWebhookUrl(s.getDiscordWebhookUrl());
            resp.setDiscordNotificationsMinSeverity(s.getDiscordNotificationsMinSeverity());
        }
        return resp;
    }

    @PostMapping("/{teamId}/notification-settings")
    public void updateNotificationSettings(
            @PathVariable @Identifier String teamId,
            @RequestBody @Valid NotificationSettingsRequest request,
            @AuthenticationPrincipal User user
    ) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        Team.NotificationSettings s = team.getNotificationSettings();
        if (s == null) s = new Team.NotificationSettings();
        String url = request.getDiscordWebhookUrl();
        s.setDiscordWebhookUrl(url == null || url.isBlank() ? null : url);
        s.setDiscordNotificationsMinSeverity(request.getDiscordNotificationsMinSeverity());
        team.setNotificationSettings(s);
        teamService.save(team);
    }

    @Data
    public static class CreateApplicationUserRequest {
        @SafeName
        private String applicationName;
        @NotNull
        private List<Permission> permissions;
    }

    @Data
    public static class CreateApplicationUserResponse {
        private String applicationName;
        private String applicationId;
        private String applicationSecret;
        private String applicationToken;
    }

    @PostMapping("/{teamId}/application-user")
    public CreateApplicationUserResponse createApplicationUser(@PathVariable @Identifier String teamId, @RequestBody @Valid CreateApplicationUserRequest request, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        CreateApplicationUserResponse response = teamService.createApplicationUser(team, request.getApplicationName(), request.getPermissions());
        eventPublisher.publishEvent(new TeamApplicationUserCreatedEvent(user.getId(), team, request.getApplicationName()));
        return response;
    }

    @GetMapping("/{teamId}/application-users")
    @ApiAccountAllowed
    public List<ApplicationDto> getApplicationUsers(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.ACCESS_TEAM);
        return teamService.getApplicationUsers(team);
    }

    @DeleteMapping("/{teamId}/application-user/{applicationId}")
    public void deleteApplicationUser(@PathVariable @Identifier String teamId, @PathVariable @Identifier String applicationId, @AuthenticationPrincipal User user) {
        Team team = teamService.getOrThrow(teamId);
        permissions.require(user, team, Permission.MANAGE_TEAM);
        teamService.deleteApplicationUser(team, applicationId);
        eventPublisher.publishEvent(new TeamApplicationUserDeletedEvent(user.getId(), team, applicationId));
    }

}
