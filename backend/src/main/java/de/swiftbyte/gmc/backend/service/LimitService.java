package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.value.LimitsConfig;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LimitService {

    public enum LimitType {
        NODES,
        SERVERS,
        BACKUPS,
        RCON_COMMANDS,
        STATISTICS,
        MEMBERS,
        SERVER_LOGS
    }

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AdminService adminService;

    // No YAML-based defaults; values come exclusively from AdminService DB config

    public int getTeamLimit(Team team, LimitType type) {
        LimitsConfig cfg = adminService.getLimitsConfig();
        int cfgNodes = cfg.teamMaxNodes;
        int cfgServers = cfg.teamMaxServers;
        int cfgBackups = cfg.serverMaxBackups;
        int cfgRcon = cfg.serverMaxRconCommands;
        int cfgStats = cfg.serverStatisticsMaxSize;
        int cfgServerLogs = cfg.serverMaxLogs;
        int cfgMembers = cfg.teamMaxMembers;

        if (team == null || team.getLimits() == null) {
            return switch (type) {
                case NODES -> cfgNodes;
                case SERVERS -> cfgServers;
                case BACKUPS -> cfgBackups;
                case RCON_COMMANDS -> cfgRcon;
                case STATISTICS -> cfgStats;
                case MEMBERS -> cfgMembers;
                case SERVER_LOGS -> cfgServerLogs;
            };
        }

        Team.Limits limits = team.getLimits();
        return switch (type) {
            case NODES -> limits.getMaxNodes() == null ? cfgNodes : limits.getMaxNodes();
            case SERVERS -> limits.getMaxServers() == null ? cfgServers : limits.getMaxServers();
            case BACKUPS -> limits.getMaxBackups() == null ? cfgBackups : limits.getMaxBackups();
            case RCON_COMMANDS -> limits.getMaxRconCommands() == null ? cfgRcon : limits.getMaxRconCommands();
            case STATISTICS -> limits.getMaxStatisticsSize() == null ? cfgStats : limits.getMaxStatisticsSize();
            case MEMBERS -> limits.getMaxMembers() == null ? cfgMembers : limits.getMaxMembers();
            case SERVER_LOGS -> cfgServerLogs; // no per-team override field defined currently
        };
    }

    public int getTeamLimit(String teamId, LimitType type) {
        Team t = teamRepository.findById(teamId).orElse(null);
        return getTeamLimit(t, type);
    }

    public int getUserOwnedTeamsLimit(User user) {
        LimitsConfig cfg = adminService.getLimitsConfig();
        int maxOwned = cfg.userMaxOwnedTeams;
        if (user != null && user.getMaxOwnedTeams() != null && user.getMaxOwnedTeams() > 0)
            return user.getMaxOwnedTeams();
        return Math.max(0, maxOwned);
    }

    public int getDefaultMaxOwnedTeamsPerUser() {
        LimitsConfig cfg = adminService.getLimitsConfig();
        int maxOwned = cfg.userMaxOwnedTeams;
        return Math.max(0, maxOwned);
    }

    public void applyTeamDefaults(Team team) {
        LimitsConfig cfg = adminService.getLimitsConfig();
        if (team.getLimits() == null) team.setLimits(new Team.Limits());
        Team.Limits limits = team.getLimits();
        if (limits.getMaxNodes() == null) limits.setMaxNodes(cfg.teamMaxNodes);
        if (limits.getMaxServers() == null) limits.setMaxServers(cfg.teamMaxServers);
        if (limits.getMaxBackups() == null) limits.setMaxBackups(cfg.serverMaxBackups);
        if (limits.getMaxRconCommands() == null) limits.setMaxRconCommands(cfg.serverMaxRconCommands);
        if (limits.getMaxStatisticsSize() == null) limits.setMaxStatisticsSize(cfg.serverStatisticsMaxSize);
        if (limits.getMaxMembers() == null) limits.setMaxMembers(cfg.teamMaxMembers);
    }
}
