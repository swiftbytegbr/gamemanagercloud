package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.aop.BlockOnNodeTasksForServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.dto.FrontendServerDto;
import de.swiftbyte.gmc.backend.events.server.ServerBackupDeletedEvent;
import de.swiftbyte.gmc.backend.events.server.ServerStateChangedEvent;
import de.swiftbyte.gmc.backend.exception.InternalServerException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.ServerException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.RefetchServerPacket;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.NodeTaskRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.SettingProfileRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.service.game.GameHandlersRegistry;
import de.swiftbyte.gmc.backend.service.server.ServerStatisticsService;
import de.swiftbyte.gmc.backend.service.setting.SettingProfileService;
import de.swiftbyte.gmc.backend.util.Generators;
import de.swiftbyte.gmc.backend.util.NetworkUtil;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.entity.RconCommand;
import de.swiftbyte.gmc.common.model.SettingProfile;
import de.swiftbyte.gmc.common.packet.from.backend.server.*;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeHeartbeatPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.*;
import de.swiftbyte.gmc.common.model.NodeTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;



@Slf4j
@Service
public class ServerService {

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private SettingProfileService profileService;

    @Autowired
    private SettingProfileRepository profileRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private StompSender stomp;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private ServerStatisticsService serverStatisticsService;

    @Autowired
    private GameHandlersRegistry gameHandlers;
    @Autowired
    private LimitService limitService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private NodeTaskRepository nodeTaskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // RCON request/response correlation registry
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CompletableFuture<de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket>> pendingRconResponses = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long DEFAULT_RCON_TIMEOUT_MS = 5000L;

    // --- HTTP Routes ---


    public List<GameServer> findByNodeId(String nodeId) {
        return serverRepository.findByNodeId(nodeId);
    }


    public List<GameServer> findByTeamId(String teamId) {
        return serverRepository.findByTeamId(teamId);
    }

    public GameServer getOrThrow(String id) {
        return serverRepository.findById(id).orElseThrow(NotFoundException::new);
    }

    public GameServer createServer(String name, GameType gameType, String map, String nodeId, String serverDirectory, boolean enablePreaquaticaBeta) {
        Node node = nodeRepository.findById(nodeId).orElseThrow(NotFoundException::new);
        if (node.getStatus() == Node.Status.OFFLINE) throw new ServerException("node_is_offline").setHttpStatus(HttpStatus.CONFLICT);

        GameServer server = new GameServer();
        server.setNodeId(nodeId);
        server.setState(GameServerState.CREATING);
        server.setCreated(Instant.now());
        server.setGameType(gameType);


        // Resolve team
        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);

        // Enforce max servers per team using a count query (index-backed)
        int maxServersPerTeam = limitService.getTeamLimit(team, LimitService.LimitType.SERVERS);
        if (maxServersPerTeam > 0 && serverRepository.countByTeamId(team.getId()) >= maxServersPerTeam) {
            throw new TeamException("too_many_servers").setHttpStatus(HttpStatus.CONFLICT);
        }

        // Enforce team-unique display name via unique index-backed exists query
        if (serverRepository.existsByTeamIdAndDisplayName(team.getId(), name))
            throw new ServerException("display_name_already_exists").setHttpStatus(HttpStatus.CONFLICT);

        server.setDisplayName(name);
        // Resolve server directory: prefer request value, fallback to node's default
        String resolvedDirectory = serverDirectory;
        if (resolvedDirectory == null || resolvedDirectory.isBlank()) {
            resolvedDirectory = (node.getSettings() != null) ? node.getSettings().getDefaultServerDirectory() : null;
        }
        if (resolvedDirectory == null || resolvedDirectory.isBlank()) throw new ServerException("server_directory_required").setHttpStatus(HttpStatus.BAD_REQUEST);
        server.setServerDirectory(resolvedDirectory);
        server.setTeamId(team.getId());

        // Create default setting profile
        Map<String, String> variables = new HashMap<>();
        variables.put("serverName", name);
        variables.put("randomRconPassword", Generators.generateRconPassword());


        var handler = gameHandlers.get(gameType);
        if (handler == null) throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);
        SettingProfile profile = handler.createDefaultProfile(variables);

        // Apply optional Preaquatica beta flag for ASE only
        if (enablePreaquaticaBeta) {
            if (gameType != GameType.ARK_EVOLVED) {
                throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);
            }
            if (profile.getGmcSettings() == null) profile.setGmcSettings(new HashMap<>());
            profile.getGmcSettings().put("EnablePreaquaticaBeta", true);
        }

        profile.setGameType(gameType.toString());
        profile.setMap(map);
        profile.setTeamId(team.getId());

        profileService.save(profile);

        server.setSettingProfileId(profile.getId());
        server = serverRepository.save(server);

        ServerCreatePacket packet = new ServerCreatePacket();
        packet.setServerId(server.getId());
        packet.setSettingProfileId(profile.getId());
        packet.setGameType(gameType);
        packet.setServerName(name);

        // include server directory for daemon to place/install
        packet.setServerDirectory(server.getServerDirectory());

        stomp.sendToNode(nodeId, "/server/create", packet);
        return server;
    }

    @BlockOnNodeTasksForServer
    public void changeServerDirectory(GameServer server, String newDirectory) {
        if (newDirectory == null || newDirectory.isBlank()) throw new ServerException("invalid_directory").setHttpStatus(HttpStatus.BAD_REQUEST);
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        server.setServerDirectory(newDirectory);
        serverRepository.save(server);

        ServerChangeDirectoryPacket packet = new ServerChangeDirectoryPacket();
        packet.setServerId(server.getId());
        packet.setServerDirectory(newDirectory);
        stomp.sendToNode(server.getNodeId(), "/server/change-directory", packet);

        // Notify frontend to refetch the server details
        RefetchServerPacket refetchPacket = new RefetchServerPacket();
        refetchPacket.setServerId(server.getId());
        stomp.sendToTeam(getTeamByServer(server), "/server/trigger-refetch", refetchPacket);
    }


    public void changeDisplayName(GameServer server, String newDisplayName) {
        if (Objects.equals(server.getDisplayName(), newDisplayName)) return;

        Node node = nodeRepository.findById(server.getNodeId()).orElseThrow(NotFoundException::new);
        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);

        if (serverRepository.existsByTeamIdAndDisplayNameAndIdNot(team.getId(), newDisplayName, server.getId()))
            throw new ServerException("display_name_already_exists").setHttpStatus(HttpStatus.CONFLICT);

        server.setDisplayName(newDisplayName);
        serverRepository.save(server);

        ServerNameChangePacket packet = new ServerNameChangePacket();
        packet.setServerId(server.getId());
        packet.setFriendlyName(newDisplayName);
        stomp.sendToNode(server.getNodeId(), "/server/name-change", packet);

        // Notify frontend to refetch the server details
        RefetchServerPacket refetchPacket = new RefetchServerPacket();
        refetchPacket.setServerId(server.getId());
        stomp.sendToTeam(getTeamByServer(server), "/server/trigger-refetch", refetchPacket);
    }

    public void resetServerSettings(GameServer server) {
        SettingProfile profile;
        Map<String, String> variables = new HashMap<>();
        variables.put("serverName", server.getDisplayName());
        variables.put("randomRconPassword", Generators.generateRconPassword());
        var handler = gameHandlers.get(server.getGameType());
        if (handler == null) throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);
        profile = handler.createDefaultProfile(variables);

        // Carry over map from existing profile and set required metadata
        SettingProfile existing = profileService.getOrThrow(server.getSettingProfileId());
        profile.setMap(existing.getMap());

        // Ensure teamId and gameType are set on the new profile
        Node node = nodeRepository.findById(server.getNodeId()).orElseThrow(NotFoundException::new);
        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);
        profile.setTeamId(team.getId());
        profile.setGameType(server.getGameType().toString());
        profile.setId(server.getSettingProfileId()); // Ensure the ID remains the same

        // Delete the old profile and save the new one
        profileRepository.deleteById(server.getSettingProfileId());
        profileService.save(profile);

        server.setSettingProfileId(profile.getId());
        serverRepository.save(server);
    }

    public int getMaxPlayers(GameServer server) {
        SettingProfile profile = profileService.getOrThrow(server.getSettingProfileId());
        var handler = gameHandlers.get(server.getGameType());
        if (handler == null) throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);
        return handler.getMaxPlayers(profile);
    }

    // --- Aggregations / DTO helpers ---

    public FrontendServerDto toDto(GameServer server) {
        Node node = nodeRepository.findById(server.getNodeId()).orElseThrow(NotFoundException::new);
        Optional<SettingProfile> profileOptional = profileRepository.findById(server.getSettingProfileId());
        if (profileOptional.isEmpty()) {
            String msg = "Failed to map GameServerDto: missing SettingProfile (serverId=" + server.getId() + ", profileId=" + server.getSettingProfileId() + ")";
            throw new InternalServerException("server.dto_mapping_profile_not_found", msg).setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        SettingProfile profile = profileOptional.get();
        var handler = gameHandlers.get(server.getGameType());
        if (handler == null) throw new ServerException("unsupported_game_type").setHttpStatus(HttpStatus.BAD_REQUEST);

        FrontendServerDto dto = objectMapper.convertValue(server, FrontendServerDto.class);

        // Enriched
        dto.setNodeName(node.getSettings() != null ? node.getSettings().getName() : null);
        List<String> ips = node.getSystemData() != null ? node.getSystemData().getIpAddresses() : List.of();
        if (ips == null) ips = List.of();
        List<String> nonLocal = NetworkUtil.filterNonLocal(ips);
        String multihome = handler.getMultihomeIp(profile);
        String serverIp = (multihome != null && !multihome.isBlank()) ? multihome : (nonLocal.isEmpty() ? null : nonLocal.get(0));
        dto.setServerIp(serverIp);
        dto.setMap(profile.getMap());
        dto.setMaxPlayers(handler.getMaxPlayers(profile));
        dto.setModCount(handler.getModCount(profile));
        dto.setServerPort(handler.getServerPort(profile));
        dto.setQueryPort(handler.getQueryPort(profile));
        dto.setRconPort(handler.getRconPort(profile));
        return dto;
    }


    // --- Server Management ---

    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public void startServer(GameServer server) {
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        ServerStartPacket packet = new ServerStartPacket();
        packet.setServerId(server.getId());
        stomp.sendToNode(server.getNodeId(), "/server/start", packet);

    }

    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public void stopServer(GameServer server, boolean forceStop, Integer delayMinutes, String message) {
        // Normal stop requires ONLINE state; force stop is allowed for any non-OFFLINE state
        if (!forceStop && server.getState() != GameServerState.ONLINE) throw new ServerException("stop_normal_invalid_state").setHttpStatus(HttpStatus.CONFLICT);

        // Force stop should not be issued if server is not ONLINE, INITIALIZING, RESTARTING, or STOPPING
        if (forceStop && server.getState() != GameServerState.ONLINE && server.getState() != GameServerState.RESTARTING && server.getState() != GameServerState.INITIALIZING && server.getState() != GameServerState.STOPPING) throw new ServerException("stop_force_already_offline").setHttpStatus(HttpStatus.CONFLICT);

        // If scheduling a timed stop, ensure no existing timed shutdown/restart task targets this server on the node
        if (delayMinutes != null && delayMinutes > 0 && hasActiveTimedTask(server))
            throw new ServerException("timed_shutdown_already_scheduled").setHttpStatus(HttpStatus.CONFLICT);

        ServerStopPacket packet = new ServerStopPacket();
        packet.setServerId(server.getId());
        packet.setForceStop(forceStop);
        packet.setDelayMinutes(delayMinutes);
        packet.setDelayedStopMessage(message);
        stomp.sendToNode(server.getNodeId(), "/server/stop", packet);
    }

    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public void restartServer(GameServer server, Integer delayMinutes, String message) {
        if (server.getState() != GameServerState.ONLINE) throw new ServerException("is_not_online").setHttpStatus(HttpStatus.CONFLICT);

        // If scheduling a timed restart, ensure no existing timed shutdown/restart task targets this server on the node
        if (delayMinutes != null && delayMinutes > 0 && hasActiveTimedTask(server))
            throw new ServerException("timed_restart_already_scheduled").setHttpStatus(HttpStatus.CONFLICT);

        ServerRestartPacket packet = new ServerRestartPacket();
        packet.setServerId(server.getId());
        packet.setDelayMinutes(delayMinutes);
        packet.setDelayedRestartMessage(message);
        stomp.sendToNode(server.getNodeId(), "/server/restart", packet);
    }

    private boolean hasActiveTimedTask(GameServer server) {
        return nodeTaskRepository.findByNodeId(server.getNodeId()).stream()
                .filter(t -> t.getType() == NodeTask.Type.TIMED_SHUTDOWN || t.getType() == NodeTask.Type.TIMED_RESTART)
                .anyMatch(t -> t.getTargetIds() != null && t.getTargetIds().contains(server.getId()));
    }


    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public void updateServer(GameServer server, boolean startAfterUpdate) {
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        ServerUpdatePacket packet = new ServerUpdatePacket();
        packet.setServerId(server.getId());
        packet.setStartAfterUpdate(startAfterUpdate);
        stomp.sendToNode(server.getNodeId(), "/server/update", packet);
    }


    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE, NodeTask.Type.BACKUP_DIRECTORY_CHANGE})
    public void backupServer(GameServer server, String name) {
        if (server.getState() == GameServerState.UNKNOWN) throw new ServerException("state_unknown").setHttpStatus(HttpStatus.CONFLICT);
        ServerBackupPacket packet = new ServerBackupPacket();
        packet.setServerId(server.getId());
        packet.setName(name);

        stomp.sendToNode(server.getNodeId(), "/server/backup", packet);
    }

    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE, NodeTask.Type.BACKUP_DIRECTORY_CHANGE})
    public void backupDeleteOldest(GameServer server) {
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        ServerDeleteBackupPacket packet = new ServerDeleteBackupPacket();
        packet.setServerId(server.getId());
        stomp.sendToNode(server.getNodeId(), "/server/delete-backup", packet);
    }

    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE, NodeTask.Type.BACKUP_DIRECTORY_CHANGE})
    public void deleteServerBackup(GameServer server, String backupId) {
        if (server.getState() == GameServerState.UNKNOWN) throw new ServerException("state_unknown").setHttpStatus(HttpStatus.CONFLICT);

        if (server.getBackups().isEmpty()) throw new ServerException("backups_empty").setHttpStatus(HttpStatus.CONFLICT);
        if (server.getBackups().stream().noneMatch(b -> b.getBackupId().equals(backupId)))
            throw new NotFoundException();
        ServerDeleteBackupPacket packet = new ServerDeleteBackupPacket();
        packet.setServerId(server.getId());
        packet.setBackupId(backupId);
        stomp.sendToNode(server.getNodeId(), "/server/delete-backup", packet);

        server.getBackups().removeIf(b -> b.getBackupId().equals(backupId));
    }

    public void deleteOldestServerBackup(GameServer server) {
        if (server.getBackups().isEmpty()) throw new ServerException("backups_empty").setHttpStatus(HttpStatus.CONFLICT);
        deleteServerBackup(server, server.getBackups().get(0).getBackupId());
    }


    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE, NodeTask.Type.BACKUP_DIRECTORY_CHANGE})
    public void rollbackServer(GameServer server, String backupId, boolean rollbackPlayers) {
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        ServerRollbackPacket packet = new ServerRollbackPacket();
        packet.setServerId(server.getId());
        packet.setBackupId(backupId);
        packet.setRollbackPlayers(rollbackPlayers);
        stomp.sendToNode(server.getNodeId(), "/server/rollback", packet);
    }


    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public void rconCommand(GameServer server, RconCommand command) {
        if (server.getState() != GameServerState.ONLINE) throw new ServerException("is_not_online").setHttpStatus(HttpStatus.CONFLICT);
        ServerRconPacket packet = new ServerRconPacket();
        packet.setServerId(server.getId());
        // Attach a correlation id on the command for potential consumers; non-await send does not track it.
        if (command.getCommandId() == null || command.getCommandId().isBlank()) {
            command.setCommandId(java.util.UUID.randomUUID().toString());
        }
        packet.setCommand(command);
        stomp.sendToNode(server.getNodeId(), "/server/rcon", packet);
    }

    /**
     * Sends an RCON command and awaits the daemon response correlated by commandId.
     * Enforces a timeout and cleans up pending entries to prevent leaks.
     */
    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE})
    public de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket rconCommandAwait(GameServer server, RconCommand command, Long timeoutMs) {
        if (server.getState() != GameServerState.ONLINE) throw new ServerException("is_not_online").setHttpStatus(HttpStatus.CONFLICT);

        String commandId = java.util.UUID.randomUUID().toString();
        command.setCommandId(commandId);
        var future = new java.util.concurrent.CompletableFuture<de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket>();
        pendingRconResponses.put(commandId, future);

        try {
            ServerRconPacket packet = new ServerRconPacket();
            packet.setServerId(server.getId());
            packet.setCommand(command);
            stomp.sendToNode(server.getNodeId(), "/server/rcon", packet);

            long waitMs = (timeoutMs == null || timeoutMs <= 0) ? DEFAULT_RCON_TIMEOUT_MS : timeoutMs;
            return future.orTimeout(waitMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                    .join();
        } catch (java.util.concurrent.CompletionException ce) {
            if (ce.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw new ServerException("rcon_timeout").setHttpStatus(HttpStatus.REQUEST_TIMEOUT);
            }
            throw ce;
        } finally {
            pendingRconResponses.remove(commandId);
        }
    }


    @BlockOnNodeTasksForServer(types = {NodeTask.Type.SERVER_DIRECTORY_CHANGE, NodeTask.Type.SAVEGAME_WIPE})
    public void wipeSavegames(GameServer server) {
        if (server.getState() != GameServerState.OFFLINE) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        ServerWipeSavegamesPacket packet = new ServerWipeSavegamesPacket();
        packet.setServerId(server.getId());
        stomp.sendToNode(server.getNodeId(), "/server/wipe-savegames", packet);
    }


    public void deleteServer(GameServer server, boolean forceDelete) {
        if (server.getState() != GameServerState.OFFLINE && !forceDelete) throw new ServerException("is_not_offline").setHttpStatus(HttpStatus.CONFLICT);
        // Respect forceDelete: only block on active directory-change tasks when not forcing
        if (!forceDelete) {
            boolean blocked = nodeTaskRepository.findByNodeId(server.getNodeId()).stream()
                    .filter(t -> t.getType() == NodeTask.Type.SERVER_DIRECTORY_CHANGE)
                    .anyMatch(t -> {
                        var targets = t.getTargetIds();
                        return targets == null || targets.isEmpty() || targets.contains(server.getId());
                    });
            if (blocked) throw new ServerException("server_directory_change_in_progress").setHttpStatus(HttpStatus.CONFLICT);
        }
        ServerDeletePacket packet = new ServerDeletePacket();
        packet.setServerId(server.getId());
        stomp.sendToNode(server.getNodeId(), "/server/delete", packet);

        server.setState(GameServerState.DELETING);
        if (forceDelete) {
            profileRepository.deleteById(server.getSettingProfileId());
            serverRepository.delete(server);
        } else serverRepository.save(server);
    }

    // Annotation-based task guards replace inline ensureNoActive* checks

    // --- Stomp Routes ---

    public void heartbeat(NodeHeartbeatPacket.GameServerUpdate data) {
        GameServer server = getOrThrow(data.getId());
        GameServerState oldState = server.getState();
        boolean stateChanged = oldState != data.getState();
        server.setState(data.getState());
        server.setOnlinePlayers(data.getPlayerCount());
        serverRepository.save(server);

        serverStatisticsService.saveStatistic(server.getId(), data.getPlayerCount(), data.getResourceUsage().getRamBytes(), data.getResourceUsage().getCpuPercentage(), data.getResourceUsage().getNetworkBytes());
        // Dispatch a state change event asynchronously for automation waits
        if(stateChanged) eventPublisher.publishEvent(new ServerStateChangedEvent("DAEMON", Instant.now(), server, oldState, data.getState()));

    }


    public void stateUpdate(ServerStatePacket packet) {
        GameServer server = getOrThrow(packet.getServerId());
        GameServerState oldState = server.getState();
        GameServerState newState = packet.getState();
        boolean stateChanged = oldState != newState;
        server.setState(newState);
        serverRepository.save(server);

        Team team = getTeamByServer(server);
        stomp.sendToTeam(team, "/server/state", packet);
        if (stateChanged) {
            eventPublisher.publishEvent(new ServerStateChangedEvent("DAEMON", Instant.now(), server, oldState, newState));
        }
    }

    public void versionChangedPacket(ServerVersionChangedPacket packet) {
        GameServer server = getOrThrow(packet.getServerId());
        server.setVersion(packet.getVersion());
        serverRepository.save(server);
    }


    public void rconResponse(ServerRconResponsePacket packet) {
        GameServer server = getOrThrow(packet.getServerId());
        // Complete correlated futures if present (via command.commandId)
        String reqId = packet.getCommand() != null ? packet.getCommand().getCommandId() : null;
        if (reqId != null && !reqId.isBlank()) {
            var fut = pendingRconResponses.remove(reqId);
            if (fut != null && !fut.isDone()) {
                fut.complete(packet);
            }
        }
        // Normalize known informal/no-response messages for better UX using RconCommand
        RconCommand cmd = packet.getCommand();
        if (cmd != null && cmd.getResponse() != null) {
            String resp = cmd.getResponse().trim();
            if (resp.equals("Server received, But no response!!")) {
                cmd.setResponse("Command received, but the server returned no output.");
            }
        }
        // Keep last N RCON commands per team limit
        Team team = getTeamByServer(server);
        int maxRcon = limitService.getTeamLimit(team, LimitService.LimitType.RCON_COMMANDS);
        if (maxRcon > 0) {
            while (server.getCommands().size() >= maxRcon) {
                server.getCommands().remove(0);
            }
        }
        server.getCommands().add(packet.getCommand());

        serverRepository.save(server);
        // Reuse team resolved above
        stomp.sendToTeam(team, "/server/rcon", packet);
    }


    public void backupResponse(ServerBackupResponsePacket packet) {
        GameServer server = getOrThrow(packet.getServerId());

        if (server.getBackups() == null) {
            server.setBackups(List.of(packet.getBackup()));
        } else {
            server.getBackups().add(packet.getBackup());
            Team team = getTeamByServer(server);
            int maxBackups = limitService.getTeamLimit(team, LimitService.LimitType.BACKUPS);
            while (maxBackups > 0 && server.getBackups().size() > maxBackups) {
                // Identify the oldest backup and publish an event attributed to BACKEND
                String oldestId = server.getBackups().get(0).getBackupId();
                String oldestName = null;
                oldestName = server.getBackups().get(0).getName();
                eventPublisher.publishEvent(new ServerBackupDeletedEvent("BACKEND", server, oldestId, oldestName));
                deleteOldestServerBackup(server);
            }
        }

        serverRepository.save(server);

        Team team = getTeamByServer(server);
        stomp.sendToTeam(team, "/server/backup", packet);
    }

    public void backupDeleteResponse(ServerDeleteBackupResponsePacket packet) {
        GameServer server = getOrThrow(packet.getServerId());
        if (server.getBackups() != null) {
            server.getBackups().removeIf(b -> b.getBackupId().equals(packet.getBackupId()));
            serverRepository.save(server);
        }

        Team team = getTeamByServer(server);
        stomp.sendToTeam(team, "/server/delete-backup", packet);
    }

    public void deleteResponse(ServerDeleteResponse packet) {
        GameServer server = getOrThrow(packet.getServerId());
        profileRepository.deleteById(server.getSettingProfileId());
        serverRepository.delete(server);


        Team team = getTeamByServer(server);
        stomp.sendToTeam(team, "/server/delete", packet);
    }

    public void changeDirectoryFailed(ServerChangeDirectoryFailedPacket packet) {
        GameServer server = getOrThrow(packet.getServerId());
        // Revert to previous directory provided by daemon
        server.setServerDirectory(packet.getPreviousDirectory());
        serverRepository.save(server);

        Team team = getTeamByServer(server);
        stomp.sendToTeam(team, "/server/directory-change-failed", packet);
    }


    public void disconnect(String id) {
        ServerStatePacket packet = new ServerStatePacket();
        packet.setState(GameServerState.UNKNOWN);
        packet.setServerId(id);

        stateUpdate(packet);
        GameServer server = getOrThrow(id);
        server.setOnlinePlayers(0);
        serverRepository.save(server);
    }


    private Team getTeamByServer(GameServer server) {
        Node node = nodeRepository.findById(server.getNodeId()).orElseThrow(NotFoundException::new);
        return teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);
    }

}
