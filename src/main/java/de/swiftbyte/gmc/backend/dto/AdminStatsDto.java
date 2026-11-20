package de.swiftbyte.gmc.backend.dto;

import lombok.Data;

@Data
public class AdminStatsDto {
    private long totalUsers;
    private long totalTeams;
    private long totalNodes;
    private long totalServers;
    private long totalPlayersOnline;
}

