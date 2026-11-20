package de.swiftbyte.gmc.backend.service.admin;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.TeamDto;
import de.swiftbyte.gmc.backend.exception.BadRequestException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.service.LimitService;
import de.swiftbyte.gmc.backend.service.TeamService;
import de.swiftbyte.gmc.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class TeamAdminService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamService teamService;
    @Autowired
    private UserService userService;
    @Autowired
    private LimitService limitService;

    public Page<TeamDto> searchTeams(String q,
                                     Integer page,
                                     Integer size,
                                     String sortKey,
                                     Sort.Direction dir) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 50 : Math.max(1, size);
        Sort sort = Sort.by(dir == null ? Sort.Direction.ASC : dir, mapSortKey(sortKey));
        Pageable pageable = PageRequest.of(p, s, sort);

        Query query = new Query();
        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            String regex = ".*" + Pattern.quote(trimmed) + ".*";

            // Find users whose email contains q (case-insensitive)
            Query userQ = new Query();
            userQ.addCriteria(Criteria.where("email").regex(regex, "i"));
            List<User> matchedUsers = mongoTemplate.find(userQ, User.class);
            List<String> matchedUserIds = matchedUsers.stream().map(User::getId).toList();

            List<Criteria> ors = new ArrayList<>();
            // Team name partial
            ors.add(Criteria.where("name").regex(regex, "i"));
            // Team ID partial
            ors.add(Criteria.where("_id").regex(regex, "i"));
            // Invite code partial (if used)
            ors.add(Criteria.where("inviteCode").regex(regex, "i"));
            // Owner ID equals q
            ors.add(Criteria.where("ownerId").is(trimmed));
            // Member ID equals q
            ors.add(Criteria.where("members." + trimmed).exists(true));
            // Owner/member email partial via matched user IDs
            if (!matchedUserIds.isEmpty()) {
                ors.add(Criteria.where("ownerId").in(matchedUserIds));
                List<Criteria> memberExistOrs = new ArrayList<>();
                for (String uid : matchedUserIds) {
                    memberExistOrs.add(Criteria.where("members." + uid).exists(true));
                }
                if (!memberExistOrs.isEmpty()) {
                    ors.add(new Criteria().orOperator(memberExistOrs.toArray(new Criteria[0])));
                }
            }
            query.addCriteria(new Criteria().orOperator(ors.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Team.class);
        query.with(pageable);
        List<Team> teams = mongoTemplate.find(query, Team.class);

        List<TeamDto> dtoList = teams.stream().map(t -> teamService.getTeamDtoOrThrow(t.getId())).toList();
        return new PageImpl<>(dtoList, pageable, total);
    }

    private String mapSortKey(String sortKey) {
        String key = (sortKey == null || sortKey.isBlank()) ? "name" : sortKey;
        return switch (key) {
            case "name" -> "name";
            case "ownerId" -> "ownerId";
            case "id" -> "_id";
            default -> "name";
        };
    }

    public TeamDto getTeamDto(String teamId) {
        return teamService.getTeamDtoOrThrow(teamId);
    }

    public void changeName(String teamId, String name) {
        Team team = teamService.getOrThrow(teamId);
        teamService.changeTeamName(team, name);
    }

    public void transferOwnership(String teamId, String newOwnerId) {
        Team team = teamService.getOrThrow(teamId);
        if (newOwnerId == null || newOwnerId.isBlank()) throw new BadRequestException("owner_required");
        // Ensure target exists
        userService.getOrThrow(newOwnerId);

        // New owner must currently be a member
        if (team.getMembers() == null || !team.getMembers().containsKey(newOwnerId)) {
            throw new TeamException("new_owner_must_be_member");
        }

        String previousOwnerId = team.getOwnerId();

        // Remove new owner from members and set as owner
        team.getMembers().remove(newOwnerId);
        team.setOwnerId(newOwnerId);

        // Previous owner becomes a member with ACCESS_TEAM permissions
        if (team.getMembers() == null) team.setMembers(new java.util.HashMap<>());
        team.getMembers().put(previousOwnerId, java.util.List.of(Permission.ACCESS_TEAM));

        teamRepository.save(team);
    }

    public void updateLimits(String teamId, Team.Limits limits) {
        Team team = teamService.getOrThrow(teamId);
        if (limits == null) limits = new Team.Limits();
        // Non-negative checks; null = fallback to defaults
        if (limits.getMaxNodes() != null && limits.getMaxNodes() < 0) throw new BadRequestException("maxNodes_invalid");
        if (limits.getMaxServers() != null && limits.getMaxServers() < 0) throw new BadRequestException("maxServers_invalid");
        if (limits.getMaxBackups() != null && limits.getMaxBackups() < 0) throw new BadRequestException("maxBackups_invalid");
        if (limits.getMaxRconCommands() != null && limits.getMaxRconCommands() < 0) throw new BadRequestException("maxRconCommands_invalid");
        if (limits.getMaxStatisticsSize() != null && limits.getMaxStatisticsSize() < 0) throw new BadRequestException("maxStatisticsSize_invalid");
        if (limits.getMaxMembers() != null && limits.getMaxMembers() < 0) throw new BadRequestException("maxMembers_invalid");
        team.setLimits(limits);
        teamRepository.save(team);
    }

    public void updateNotificationSettings(String teamId, Team.NotificationSettings settings) {
        Team team = teamService.getOrThrow(teamId);
        if (settings == null) settings = new Team.NotificationSettings();
        // Default minimum severity if URL present but severity null
        if (settings.getDiscordNotificationsMinSeverity() == null && settings.getDiscordWebhookUrl() != null && !settings.getDiscordWebhookUrl().isBlank()) {
            settings.setDiscordNotificationsMinSeverity(de.swiftbyte.gmc.common.model.NotificationSeverity.ERROR);
        }
        String url = settings.getDiscordWebhookUrl();
        settings.setDiscordWebhookUrl(url == null || url.isBlank() ? null : url);
        team.setNotificationSettings(settings);
        teamRepository.save(team);
    }

    public void changeIcon(String teamId, byte[] bytes) {
        Team team = teamService.getOrThrow(teamId);
        teamService.changeTeamIconFromBytes(team, bytes);
    }

    public void addMember(String teamId, String userId, List<Permission> permissions) {
        Team team = teamService.getOrThrow(teamId);
        if (team.getMembers() == null) team.setMembers(new java.util.HashMap<>());
        if (team.getMembers().containsKey(userId)) throw new TeamException("already_member");
        // Enforce max members capacity
        int maxMembers = limitService.getTeamLimit(team, LimitService.LimitType.MEMBERS);
        int memberCount = team.getMembers() == null ? 0 : team.getMembers().size();
        if (maxMembers > 0 && memberCount >= maxMembers) throw new TeamException("too_many_members");
        userService.getOrThrow(userId);
        team.getMembers().put(userId, permissions == null ? List.of(Permission.ACCESS_TEAM) : permissions);
        teamRepository.save(team);
    }

    public void removeMember(String teamId, String userId) {
        Team team = teamService.getOrThrow(teamId);
        if (team.getOwnerId().equals(userId)) throw new TeamException("cannot_remove_owner");
        if (team.getMembers() != null) {
            team.getMembers().remove(userId);
            teamRepository.save(team);
        }
    }

    public void changeMemberPermissions(String teamId, String userId, List<Permission> perms) {
        Team team = teamService.getOrThrow(teamId);
        User member = userService.getOrThrow(userId);
        teamService.changeMemberPermission(team, member, perms);
    }

    public void deleteTeamCascade(String teamId, String confirm) {
        if (!"delete".equalsIgnoreCase(confirm)) throw new BadRequestException("delete_confirmation_required");
        Team team = teamService.getOrThrow(teamId);
        teamService.deleteTeamCascade(team);
    }
}
