package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.controller.TeamController;
import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.*;
import de.swiftbyte.gmc.backend.repository.ApiUserRepository;
import de.swiftbyte.gmc.backend.dto.ApplicationDto;
import de.swiftbyte.gmc.backend.dto.TeamDto;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.util.Generators;
import de.swiftbyte.gmc.backend.util.StringSanitizer;
import de.swiftbyte.gmc.common.packet.from.backend.team.TeamChangePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class TeamService {

    // Domain of the generated logins of API applications, see gmc.applications.email-domain
    @Value("${gmc.applications.email-domain:applications.invalid}")
    private String applicationsEmailDomain;

    // === Dependencies ===
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private EmailSender emailService;
    @Autowired
    private UserService userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.ServerRepository serverRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.SettingProfileRepository settingProfileRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.NotificationRepository notificationRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.server.ServerStatisticsRepository serverStatisticsRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.AutomationRepository automationRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.AutomationRunRepository automationRunRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.TeamActionLogRepository teamActionLogRepository;
    @Autowired
    private de.swiftbyte.gmc.backend.repository.NodeTaskRepository nodeTaskRepository;

    @Autowired
    private LimitService limitService;
    @Autowired
    private StompSender stompSender;
    @Autowired
    private ImageService imageService;


    // === Team Retrieval ===

    /**
     * Get a team by ID or throw if not found.
     */
    public Team getOrThrow(String teamId) {
        Optional<Team> optional = teamRepository.findById(teamId);
        if (optional.isEmpty()) throw new TeamException("not_found");
        return optional.get();
    }

    // === Team Creation ===

    /**
     * Create a new team with the given name and owner.
     */
    public Team createTeam(String name, User owner) {
        if (!owner.isEmailVerified()) throw new TeamException("email_not_verified");
        int userTeams = teamRepository.findByOwnerId(owner.getId()).size();
        int maxOwned = limitService.getUserOwnedTeamsLimit(owner);
        if (maxOwned > 0 && userTeams >= maxOwned) throw new TeamException("too_many_teams");

        Team team = new Team();
        team.setName(name);
        team.setOwnerId(owner.getId());
        team.setMembers(new HashMap<>());
        team.setInviteCode(Generators.generateVerificationToken().toString());
        team.setInvitedMembers(new HashMap<>());

        // Initialize team-specific limits with defaults
        limitService.applyTeamDefaults(team);
        team = teamRepository.save(team);

        return team;
    }

    // === Team Membership Management ===

    /**
     * Join a team using an invite code.
     */
    public Team joinTeam(String inviteCode, User user) {
        Optional<Team> optional = teamRepository.findByInviteCode(inviteCode);
        if (!user.isEmailVerified()) throw new TeamException("email_not_verified");
        if (optional.isEmpty()) throw new TeamException("invitation_invalid");
        Team team = optional.get();
        // Enforce max members per team (excludes owner). 0 means unlimited.
        int maxMembers = limitService.getTeamLimit(team, LimitService.LimitType.MEMBERS);
        int memberCount = team.getMembers() == null ? 0 : team.getMembers().size();
        if (maxMembers > 0 && memberCount >= maxMembers) throw new TeamException("too_many_members");
        if (team.getInvitedMembers() == null || !team.getInvitedMembers().containsKey(user.getId()))
            throw new TeamException("invitation_invalid");
        Instant invitedAt = team.getInvitedMembers().get(user.getId());
        if (invitedAt.isBefore(Instant.now().minusSeconds(14L * 24 * 60 * 60)))
            throw new TeamException("invitation_expired");
        team.getInvitedMembers().remove(user.getId());
        team.getMembers().put(user.getId(), List.of(Permission.ACCESS_TEAM));
        teamRepository.save(team);

        notifyTeamChange(team);

        return team;
    }

    /**
     * Invite a user to a team by email.
     */
    public void inviteMember(Team team, String userEmail) {
        User user = userService.findByEmail(userEmail).orElseThrow(NotFoundException::new);
        if (team.getMembers().containsKey(user.getId())) throw new TeamException("already_member");
        // Prevent inviting beyond max members capacity
        int maxMembers = limitService.getTeamLimit(team, LimitService.LimitType.MEMBERS);
        int memberCount = team.getMembers() == null ? 0 : team.getMembers().size();
        if (maxMembers > 0 && memberCount >= maxMembers) throw new TeamException("too_many_members");
        if (team.getInvitedMembers() == null) team.setInvitedMembers(new HashMap<>());
        // (Re-)invite: set the timestamp to now
        team.getInvitedMembers().put(user.getId(), Instant.now());
        emailService.sendTeamInviteEmail(user.getEmail(), team);
        teamRepository.save(team);

        notifyTeamChange(team);
    }

    /**
     * Change a member's permissions in a team.
     */
    public void changeMemberPermission(Team team, User member, List<Permission> permissions) {
        if (!team.getMembers().containsKey(member.getId())) throw new TeamException("not_member");
        team.getMembers().put(member.getId(), permissions);
        teamRepository.save(team);

        notifyTeamChange(team);
    }

    /**
     * Leave a team as a member (not owner).
     */
    public void leaveTeam(User user, Team team) {
        if (team.getOwnerId().equals(user.getId())) throw new TeamException("cannot_leave");
        team.getMembers().remove(user.getId());
        teamRepository.save(team);
        // Action log is emitted by the controller via events

        notifyTeamChange(team);
    }

    /**
     * Kick a member from a team.
     */
    public void kickMember(Team team, String userId) {
        team.getMembers().remove(userId);
        teamRepository.save(team);

        notifyTeamChange(team);
    }

    // === Team Deletion ===

    /**
     * Delete a team.
     */
    public void deleteTeam(Team team) {
        // Prevent deletion if there are still nodes registered to this team
        if (!nodeRepository.findByTeamId(team.getId()).isEmpty()) {
            throw new TeamException("deletion_failed_has_nodes");
        }
        teamRepository.delete(team);
    }

    /**
     * Delete a team and all associated data: nodes, servers, settings, statistics, automations, notifications, action logs, and application users.
     */
    public void deleteTeamCascade(Team team) {
        String teamId = team.getId();

        // 1) Delete server statistics in batches using paged servers
        int page = 0;
        int size = 500;
        Page<de.swiftbyte.gmc.backend.domain.model.GameServer> serverPage;
        do {
            serverPage = serverRepository.findByTeamId(teamId, PageRequest.of(page, size));
            var ids = serverPage.getContent().stream().map(GameServer::getId).toList();
            if (!ids.isEmpty()) serverStatisticsRepository.deleteByServerIdIn(ids);
            page++;
        } while (!serverPage.isEmpty());

        // 2) Delete servers in one repository operation
        serverRepository.deleteByTeamId(teamId);

        // 3) Delete node tasks by batching node IDs and then delete nodes
        int nodePage = 0;
        Page<Node> pagedNodes;
        do {
            pagedNodes = nodeRepository.findByTeamId(teamId, PageRequest.of(nodePage, size));
            var nodeIds = pagedNodes.getContent().stream().map(Node::getId).toList();
            if (!nodeIds.isEmpty()) nodeTaskRepository.deleteByNodeIdIn(nodeIds);
            nodePage++;
        } while (!pagedNodes.isEmpty());
        nodeRepository.deleteByTeamId(teamId);

        // 4) Delete setting profiles for team
        settingProfileRepository.deleteByTeamId(teamId);

        // 5) Delete automations and runs
        automationRunRepository.deleteByTeamId(teamId);
        automationRepository.deleteByTeamId(teamId);

        // 6) Delete notifications
        notificationRepository.deleteByTeamId(teamId);

        // 7) Delete action logs via bulk delete
        try { teamActionLogRepository.deleteByTeamId(teamId); } catch (Exception ignored) {}

        // 8) Delete application users referenced by team (map lives on team doc)
        if (team.getApplications() != null && !team.getApplications().isEmpty()) {
            for (String appUserId : team.getApplications().keySet()) {
                apiUserRepository.findById(appUserId).ifPresent(apiUserRepository::delete);
            }
        }

        // 9) Delete stored team icon image
        if (team.getIconImageId() != null && !team.getIconImageId().isBlank()) {
            imageService.deleteById(team.getIconImageId());
        }

        // 10) Finally, delete team
        teamRepository.delete(team);
    }

    // === Team Querying ===

    /**
     * Get all teams a user is part of (owner or member).
     */
    public List<Team> getTeamsByUser(User user) {
        List<Team> ownerTeams = teamRepository.findByOwnerId(user.getId());
        List<Team> memberTeams = teamRepository.findByMembersContaining(user.getId());
        return Stream.of(ownerTeams, memberTeams).flatMap(List::stream).toList();
    }

    /**
     * Get all teams a user is part of (owner, member, or application user).
     */
    public List<Team> getAllTeamsByUser(User user) {
        return teamRepository.findAllByUserIdIncludingApplications(user.getId());
    }

    // === Team Attribute Changes ===

    /**
     * Change the team's name.
     */
    public void changeTeamName(Team team, String name) {
        team.setName(name);
        teamRepository.save(team);

        TeamChangePacket teamChangePacket = new TeamChangePacket();
        teamChangePacket.setTeamId(team.getId());
        teamChangePacket.setTeamName(name);
        stompSender.sendToTeam(team, "/team/change", teamChangePacket);
        stompSender.sendToNodesByTeam(team, "/team/change", teamChangePacket);
    }

    /**
     * Change the team's icon from raw image bytes.
     */
    public void changeTeamIconFromBytes(Team team, byte[] bytes) {
        // Store new image and delete previous
        String oldImageId = team.getIconImageId();
        String newImageId = imageService.saveBytes(bytes);
        team.setIconImageId(newImageId);
        // Clear legacy base64 to reduce storage duplication
        team.setIconBase64(null);
        teamRepository.save(team);
        if (oldImageId != null && !oldImageId.isBlank()) {
            imageService.deleteById(oldImageId);
        }

        notifyTeamChange(team);
    }

    /**
     * Legacy support when only base64 is available.
     */
    public void changeTeamIcon(Team team, String iconBase64) {
        String raw = ImageService.normalizeBase64(iconBase64);
        byte[] bytes = raw == null ? new byte[0] : java.util.Base64.getMimeDecoder().decode(raw);
        changeTeamIconFromBytes(team, bytes);
    }

    /**
     * Send a generic team change update to team members.
     * Excludes name changes, which include extra fields and node notifications.
     */
    private void notifyTeamChange(Team team) {
        TeamChangePacket packet = new TeamChangePacket();
        packet.setTeamId(team.getId());
        stompSender.sendToTeam(team, "/team/change", packet);
    }

    /**
     * Persist updates to a team entity.
     */
    public void save(Team team) {
        teamRepository.save(team);
    }

    // Action log methods moved to TeamActionLogService; use that directly.

    // === Application User Management ===

    /**
     * Create an application user for a team.
     */
    public TeamController.CreateApplicationUserResponse createApplicationUser(Team team, String applicationName, List<Permission> permissions) {
        String local = StringSanitizer.emailSafePart(applicationName);
        String domainTeam = StringSanitizer.emailSafePart(team.getName() == null ? "" : team.getName().toLowerCase());
        String applicationEmail = local + "@" + domainTeam + "." + applicationsEmailDomain;
        // Avoid duplicates across API user collection
        if (apiUserRepository.findByEmail(applicationEmail).isPresent())
            throw new TeamException("application_already_exists");
        String applicationSecret = Generators.generateApplicationSecret();
        ApiUser user = new ApiUser();
        user.setEmail(applicationEmail);
        user.setPassword(passwordEncoder.encode(applicationSecret));
        user.setTwoFAType(User.TwoFAType.NONE);
        user.setEmailVerified(true);
        user.setAccountType(User.AccountType.USER);
        apiUserRepository.save(user);
        if (team.getApplications() == null) team.setApplications(new HashMap<>());
        team.getApplications().put(user.getId(), permissions);
        teamRepository.save(team);
        TeamController.CreateApplicationUserResponse response = new TeamController.CreateApplicationUserResponse();
        response.setApplicationName(applicationName);
        response.setApplicationId(user.getId());
        response.setApplicationSecret(applicationSecret);
        response.setApplicationToken("gmc-app-token-" + user.getId() + ":" + applicationSecret);
        return response;
    }

    /**
     * Get all application users for a team.
     */
    public List<ApplicationDto> getApplicationUsers(Team team) {
        List<ApplicationDto> applications = new ArrayList<>();
        if (team.getApplications() == null) return applications;
        for (String userId : team.getApplications().keySet()) {
            apiUserRepository.findById(userId).ifPresent(u -> applications.add(fromModel(u, team)));
        }
        return applications;
    }

    /**
     * Delete an application user from a team.
     */
    public void deleteApplicationUser(Team team, String applicationId) {
        Optional<ApiUser> userOptional = apiUserRepository.findById(applicationId);
        if (userOptional.isEmpty()) throw new TeamException("application_not_found");
        ApiUser user = userOptional.get();
        if (team.getApplications() != null) team.getApplications().remove(user.getId());
        teamRepository.save(team);
        apiUserRepository.delete(user);
    }

    // === DTO Conversion ===

    /**
     * Convert a User and Team to ApplicationDto.
     */
    private ApplicationDto fromModel(User user, Team team) {
        ApplicationDto dto = new ApplicationDto();
        dto.setApplicationId(user.getId());
        dto.setName(user.getEmail().split("@")[0]);
        dto.setPermissions(team.getApplications() == null ? List.of() : team.getApplications().get(user.getId()));
        return dto;
    }

    /**
     * Convert a Team to TeamDto.
     */
    public TeamDto fromModel(Team team) {
        TeamDto dto = new TeamDto();
        dto.setId(team.getId());
        dto.setName(team.getName());
        dto.setIconBase64(team.getIconBase64());
        dto.setInviteCode(team.getInviteCode());
        dto.setMembers(new ArrayList<>());
        // Fill invited members DTOs
        List<TeamDto.TeamDtoInvite> inviteDtos = new ArrayList<>();
        if (team.getInvitedMembers() != null) {
            for (String invitedUserId : team.getInvitedMembers().keySet()) {
                Optional<User> userOptional = userService.findById(invitedUserId);
                if (userOptional.isPresent()) {
                    User u = userOptional.get();
                    TeamDto.TeamDtoInvite inv = new TeamDto.TeamDtoInvite();
                    inv.setId(u.getId());
                    inv.setName(u.getPersonalInformation().getUsername());
                    inv.setEmail(u.getEmail());
                    inv.setIconBase64(u.getPersonalInformation().getAvatarBase64());
                    inv.setImageId(u.getAvatarImageId());
                    Instant invitedAt = team.getInvitedMembers().get(invitedUserId);
                    inv.setInvitedAt(invitedAt);
                    boolean expired = invitedAt.isBefore(Instant.now().minusSeconds(14L * 24 * 60 * 60));
                    inv.setExpired(expired);
                    inviteDtos.add(inv);
                }
            }
        }
        dto.setInvitedMembers(inviteDtos);
        for (String userId : team.getMembers().keySet()) {
            Optional<User> userOptional = userService.findById(userId);
            if (userOptional.isPresent()) {
                dto.getMembers().add(getTeamDtoMember(team, userId));
            }
        }
        Optional<User> ownerOptional = userService.findById(team.getOwnerId());
        dto.setOwner(ownerOptional.isPresent() ? getTeamDtoMember(team, team.getOwnerId()) : null);
        dto.setIconImageId(team.getIconImageId());
        // Expose effective limits to DTO (nested limits object)
        dto.setLimits(team.getLimits());
        dto.setNotificationSettings(team.getNotificationSettings());
        return dto;
    }

    /**
     * Revoke an outstanding invite.
     */
    public void revokeInvite(Team team, String userEmail) {
        if (team.getInvitedMembers() == null || !team.getInvitedMembers().containsKey(userEmail))
            throw new TeamException("invitation_invalid");
        team.getInvitedMembers().remove(userEmail);
        teamRepository.save(team);
    }

    /**
     * Daily cleanup of invites older than 21 days.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupExpiredInvites() {
        List<Team> teams = teamRepository.findAll();
        Instant cutoff = Instant.now().minusSeconds(21L * 24 * 60 * 60);
        for (Team t : teams) {
            if (t.getInvitedMembers() == null || t.getInvitedMembers().isEmpty()) continue;
            boolean changed = false;
            List<String> toRemove = new ArrayList<>();
            for (var entry : t.getInvitedMembers().entrySet()) {
                if (entry.getValue() != null && entry.getValue().isBefore(cutoff)) {
                    toRemove.add(entry.getKey());
                }
            }
            if (!toRemove.isEmpty()) {
                toRemove.forEach(t.getInvitedMembers()::remove);
                teamRepository.save(t);
            }
        }
    }

    private TeamDto.TeamDtoMember getTeamDtoMember(Team team, String userId) {
        Optional<User> userOptional = userService.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            TeamDto.TeamDtoMember member = new TeamDto.TeamDtoMember();
            member.setId(user.getId());
            member.setName(user.getPersonalInformation().getUsername());
            member.setEmail(user.getEmail());
            member.setIconBase64(user.getPersonalInformation().getAvatarBase64());
            member.setImageId(user.getAvatarImageId());
            member.setPermissions(team.getOwnerId().equals(userId) ? List.of(Permission.values()) : team.getMembers().get(userId));
            // 2FA is enabled if the user's TwoFAType is not NONE
            member.setTwoFAEnabled(user.getTwoFAType() != null && user.getTwoFAType() != User.TwoFAType.NONE);
            return member;
        }
        return null;
    }

    public TeamDto getTeamDtoOrThrow(String teamId) {
        Team team = getOrThrow(teamId);
        return fromModel(team);
    }

    public List<TeamDto> getTeamDtosByUser(User user) {
        List<TeamDto> teamDtos = new ArrayList<>();
        for (Team team : getTeamsByUser(user)) {
            teamDtos.add(fromModel(team));
        }
        return teamDtos;
    }

    public List<TeamDto> getTeamDtosByUserIncludingApplications(User user) {
        List<TeamDto> dtos = new ArrayList<>();
        for (Team team : getAllTeamsByUser(user)) {
            dtos.add(fromModel(team));
        }
        return dtos;
    }


    @Autowired
    private ApiUserRepository apiUserRepository;
}
