package de.swiftbyte.gmc.backend.domain.value;

// Holds default limit values configured by admins.
public class LimitsConfig {
    public int teamMaxNodes;
    public int teamMaxServers;
    public int serverMaxBackups;
    public int serverMaxRconCommands;
    public int serverStatisticsMaxSize;
    public int teamMaxMembers; // 0 means unlimited
    public int userMaxOwnedTeams;
    public int serverMaxLogs; // 0 means unlimited
}
