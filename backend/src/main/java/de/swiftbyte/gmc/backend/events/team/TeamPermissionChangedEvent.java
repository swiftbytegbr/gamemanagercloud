package de.swiftbyte.gmc.backend.events.team;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import java.util.Objects;

@Getter
@ActionLogType(ActionLogItem.Type.TEAM_PERMISSION_CHANGE)
public class TeamPermissionChangedEvent extends TeamEvent implements ActionLogEvent {
    private final String affectedUserId;
    private final List<Permission> oldPermissions;
    private final List<Permission> newPermissions;

    public TeamPermissionChangedEvent(String issuerId, Team team, String affectedUserId, List<Permission> oldPermissions, List<Permission> newPermissions) { this(issuerId, Instant.now(), team, affectedUserId, oldPermissions, newPermissions); }
    public TeamPermissionChangedEvent(String issuerId, Instant timestamp, Team team, String affectedUserId, List<Permission> oldPermissions, List<Permission> newPermissions) {
        super(issuerId, timestamp, team);
        this.affectedUserId = affectedUserId;
        this.oldPermissions = oldPermissions;
        this.newPermissions = newPermissions;
        String permsChange = formatPermissions(oldPermissions) + " -> " + formatPermissions(newPermissions);
        setActionLogInformation("Permissions updated: " + permsChange);
    }

    private static String formatPermissions(List<Permission> perms) {
        if (perms == null || perms.isEmpty()) return "No permissions";
        boolean accessTeam = perms.contains(Permission.ACCESS_TEAM);
        boolean manageTeam = perms.contains(Permission.MANAGE_TEAM);
        boolean accessNodes = perms.contains(Permission.ACCESS_NODES);
        boolean manageNodes = perms.contains(Permission.MANAGE_NODES);
        boolean accessServers = perms.contains(Permission.ACCESS_SERVERS);
        boolean manageServers = perms.contains(Permission.MANAGE_SERVERS);
        boolean accessProfiles = perms.contains(Permission.ACCESS_PROFILES);
        boolean manageProfiles = perms.contains(Permission.MANAGE_PROFILES);

        // Merge PROFILE permissions into SERVER permissions for display
        boolean manageServersOrProfiles = manageServers || manageProfiles;
        boolean accessServersOrProfiles = accessServers || accessProfiles;

        String team = manageTeam ? "Manage Team" : (accessTeam ? "Access Team" : null);
        String nodes = manageNodes ? "Manage Nodes" : (accessNodes ? "Access Nodes" : null);
        String servers = manageServersOrProfiles ? "Manage Servers" : (accessServersOrProfiles ? "Access Servers" : null);

        return Stream.of(team, nodes, servers)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "; " + b)
                .orElse("No permissions");
    }
}
