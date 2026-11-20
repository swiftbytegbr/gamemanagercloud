package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.exception.ForbiddenException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.BadRequestException;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class PermissionService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ServerRepository serverRepository;

    // Cache only stable mappings (no user-specific permissions): nodeId/serverId -> teamId
    private static class TeamIdCacheEntry {
        String teamId;
        long expiresAt;
    }

    private final ConcurrentHashMap<String, TeamIdCacheEntry> nodeToTeamCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TeamIdCacheEntry> serverToTeamCache = new ConcurrentHashMap<>();

    // TTL is configurable via YAMLs
    @Value("${gmc.security.permission-cache.ttl-millis:60000}")
    private long cacheTtlMillis;

    private String resolveTeamIdByNodeId(String nodeId) {
        TeamIdCacheEntry entry = nodeToTeamCache.get(nodeId);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt > now) {
            return entry.teamId;
        }
        Node node = nodeRepository.findById(nodeId).orElseThrow(NotFoundException::new);
        TeamIdCacheEntry fresh = new TeamIdCacheEntry();
        fresh.teamId = node.getTeamId();
        fresh.expiresAt = now + cacheTtlMillis;
        nodeToTeamCache.put(nodeId, fresh);
        return fresh.teamId;
    }

    private String resolveTeamIdByServerId(String serverId) {
        TeamIdCacheEntry entry = serverToTeamCache.get(serverId);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt > now) {
            return entry.teamId;
        }
        // Resolve via server -> node -> team
        String teamId = serverRepository.findById(serverId)
                .map(s -> resolveTeamIdByNodeId(s.getNodeId()))
                .orElseThrow(NotFoundException::new);
        TeamIdCacheEntry fresh = new TeamIdCacheEntry();
        fresh.teamId = teamId;
        fresh.expiresAt = now + cacheTtlMillis;
        serverToTeamCache.put(serverId, fresh);
        return teamId;
    }

    // Allow external invalidation when servers are moved or deleted
    public void invalidateServerCache(String serverId) {
        serverToTeamCache.remove(serverId);
    }

    public void invalidateNodeCache(String nodeId) {
        nodeToTeamCache.remove(nodeId);
    }

    public void requireGmcAdmin(User user) {
        if (user.getAccountType() != User.AccountType.ADMIN) throw new ForbiddenException("admin_only");
        // Admin area additionally requires 2FA to be enabled
        if (user.getTwoFAType() == null || user.getTwoFAType() == User.TwoFAType.NONE) {
            throw new ForbiddenException("admin_twofa_required");
        }
    }

    public void require(User user, Team team, Permission permission) {
        if (team.getOwnerId().equals(user.getId())) return;
        // Check normal members
        if (team.getMembers() != null && team.getMembers().containsKey(user.getId())) {
            var perms = team.getMembers().get(user.getId());
            if (perms != null && (perms.contains(Permission.ALL) || perms.contains(permission))) return;
        }
        // Check application users
        if (team.getApplications() != null && team.getApplications().containsKey(user.getId())) {
            var perms = team.getApplications().get(user.getId());
            if (perms != null && (perms.contains(Permission.ALL) || perms.contains(permission))) return;
        }

        // Check admin users
        if (user.getAccountType() == User.AccountType.ADMIN) return;
        throw new ForbiddenException(permission);
    }

    public void require(User user, Node object, Permission permission) {
        Team team = teamRepository.findById(object.getTeamId()).orElseThrow(NotFoundException::new);
        require(user, team, permission);
    }

    public void requireOwner(User user, Team team) {
        if (!team.getOwnerId().equals(user.getId())) throw new ForbiddenException("owner_only");
    }

    public void require(User user, GameServer object, Permission permission) {
        String teamId = resolveTeamIdByNodeId(object.getNodeId());
        requireByTeamId(user, teamId, permission);
    }


    public void requireByNodeId(User user, String nodeId, Permission permission) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new BadRequestException("node_id_missing");
        }
        String teamId = resolveTeamIdByNodeId(nodeId);
        requireByTeamId(user, teamId, permission);
    }

    public void requireByServerId(User user, String serverId, Permission permission) {
        String teamId = resolveTeamIdByServerId(serverId);
        requireByTeamId(user, teamId, permission);
    }


    public void requireByTeamIdAndAllowNodeAccess(User user, String teamId, Permission permission, Authentication authentication) {
        Optional<Node> nodeAuth = nodeRepository.findById(authentication.getName());
        if (nodeAuth.isPresent() && nodeAuth.get().getTeamId().equals(teamId)) return;
        if (user == null) throw new ForbiddenException(permission);
        requireByTeamId(user, teamId, permission);
    }


    public void requireByTeamId(User user, String teamId, Permission permission) {
        Team team = teamRepository.findById(teamId).orElseThrow(NotFoundException::new);
        require(user, team, permission);
    }


}
