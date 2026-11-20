package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.FrontendServerDto;
import de.swiftbyte.gmc.backend.events.server.*;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.ServerService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import de.swiftbyte.gmc.backend.validation.SafeName;
import de.swiftbyte.gmc.common.entity.Backup;
import de.swiftbyte.gmc.common.entity.GameType;
import de.swiftbyte.gmc.common.entity.RconCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/server")
@Validated
@ApiAccountAllowed
public class ServerController {

    @Autowired
    private ServerService serverService;


    @Autowired
    private PermissionService permissions;

    @Autowired
    private ApplicationEventPublisher eventPublisher;


    @GetMapping("/{serverId}")
    public FrontendServerDto get(
            @PathVariable @Identifier String serverId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.ACCESS_SERVERS);
        return serverService.toDto(server);
    }


    @GetMapping("/by-node/{nodeId}")
    public List<FrontendServerDto> getByNodeId(
            @PathVariable @Identifier String nodeId,
            @AuthenticationPrincipal User user) {
        permissions.requireByNodeId(user, nodeId, Permission.ACCESS_SERVERS);
        return serverService.findByNodeId(nodeId).stream().map(serverService::toDto).toList();
    }


    @GetMapping("/by-team/{teamId}")
    public List<FrontendServerDto> getByTeamId(
            @PathVariable @Identifier String teamId,
            @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_SERVERS);
        return serverService.findByTeamId(teamId).stream().map(serverService::toDto).toList();
    }

    @Data
    public static class CreateServerRequest {
        @SafeName
        private String name;
        @NotNull
        private GameType gameType;
        @Size(max = 64)
        private String map;
        @Identifier
        private String nodeId;
        private Boolean enablePreaquaticaBeta;
        // Optional: if omitted, node's default directory is used
        //TODO @ServerDirectoryPath
        private String serverDirectory;
    }

    @PostMapping("/create")
    public GameServer createServer(@RequestBody @Valid CreateServerRequest request, @AuthenticationPrincipal User user) {
        permissions.requireByNodeId(user, request.getNodeId(), Permission.MANAGE_SERVERS);
        boolean preaquatica = request.getEnablePreaquaticaBeta() != null && request.getEnablePreaquaticaBeta();
        GameServer gameServer = serverService.createServer(request.getName(), request.getGameType(), request.getMap(), request.getNodeId(), request.getServerDirectory(), preaquatica);

        eventPublisher.publishEvent(new ServerCreatedEvent(user.getId(), gameServer));

        return gameServer;
    }

    @Data
    public static class ChangeDirectoryRequest {
        @NotBlank
        //TODO @ServerDirectoryPath
        private String serverDirectory;
    }

    @PostMapping("/{serverId}/change-directory")
    public void changeDirectory(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid ChangeDirectoryRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        String old = server.getServerDirectory();
        serverService.changeServerDirectory(server, request.getServerDirectory());

        eventPublisher.publishEvent(new ServerSettingsChangedEvent(
                user.getId(), server,
                "Changed server directory from \"" + (old == null ? "" : old) + "\" to \"" + request.getServerDirectory() + "\""
        ));
    }

    @GetMapping("/{serverId}/maxPlayers")
    public int getMaxPlayers(
            @PathVariable @Identifier String serverId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.ACCESS_SERVERS);
        return serverService.getMaxPlayers(server);
    }

    @PostMapping("/{serverId}/start")
    public void start(
            @PathVariable @Identifier String serverId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.startServer(server);

        eventPublisher.publishEvent(new ServerStartRequestEvent(user.getId(), server));
    }

    @Data
    public static class StopServerRequest {
        private boolean forceStop;
        // Optional delay in minutes for scheduled shutdown
        private Integer delayMinutes;
        // Optional custom stop message
        private String delayedStopMessage;
    }

    @PostMapping("/{serverId}/stop")
    public void stop(
            @PathVariable @Identifier String serverId,
            @RequestBody StopServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.stopServer(server, request.isForceStop(), request.getDelayMinutes(), request.delayedStopMessage);

        eventPublisher.publishEvent(new ServerStopRequestEvent(user.getId(), server, request.isForceStop()));
    }

    @Data
    public static class RestartServerRequest {
        // Optional delay in minutes for scheduled restart
        private Integer delayMinutes;
        // Optional custom stop message (used prior to restart)
        private String delayedRestartMessage;
    }

    @PostMapping("/{serverId}/restart")
    public void restart(
            @PathVariable @Identifier String serverId,
            @RequestBody(required = false) RestartServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        Integer delay = request == null ? null : request.getDelayMinutes();
        String msg = request == null ? null : request.getDelayedRestartMessage();
        serverService.restartServer(server, delay, msg);

        eventPublisher.publishEvent(new ServerRestartRequestEvent(user.getId(), server));
    }

    @Data
    public static class UpdateServerRequest {
        private boolean startAfterUpdate;
    }

    @PostMapping("/{serverId}/update")
    public void update(
            @PathVariable @Identifier String serverId,
            @RequestBody UpdateServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.updateServer(server, request.isStartAfterUpdate());

        eventPublisher.publishEvent(new ServerUpdateRequestEvent(user.getId(), server));
    }

    @Data
    public static class BackupServerRequest {
        @SafeName
        private String name;
    }

    @PostMapping("/{serverId}/backup")
    public void backup(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid BackupServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.backupServer(server, request.getName());

        eventPublisher.publishEvent(new ServerBackupRequestEvent(user.getId(), server, request.getName()));
    }

    @Data
    public static class DeleteBackupRequest {
        @Identifier
        private String backupId;
    }

    @PostMapping("/{serverId}/delete-backup")
    public void deleteBackup(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid DeleteBackupRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.deleteServerBackup(server, request.getBackupId());

        String backupName = null;
        if (server.getBackups() != null) {
            Backup match = server.getBackups().stream().filter(b -> b.getBackupId().equals(request.getBackupId())).findFirst().get();
            backupName = match.getName();
        }

        eventPublisher.publishEvent(new ServerBackupDeletedEvent(user.getId(), server, request.getBackupId(), backupName));
    }

    @Data
    public static class RollbackServerRequest {
        @Identifier
        private String backupId;
        private boolean rollbackPlayers;
    }

    @PostMapping("/{serverId}/rollback")
    public void rollback(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid RollbackServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.rollbackServer(server, request.getBackupId(), request.isRollbackPlayers());

        String rbName = null;
        if (server.getBackups() != null) {
            Backup match = server.getBackups().stream().filter(b -> b.getBackupId().equals(request.getBackupId())).findFirst().get();
            rbName = match.getName();
        }

        eventPublisher.publishEvent(new ServerRollbackRequestEvent(user.getId(), server, request.getBackupId(), rbName, request.isRollbackPlayers()));
    }

    @PostMapping("/{serverId}/rcon")
    public void rcon(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid RconCommand command,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.rconCommand(server, command);

        eventPublisher.publishEvent(new ServerRconCommandRequestEvent(user.getId(), server, command.getCommand()));
    }

    // Awaitable variant gated by query param: ?await=true[&timeoutMs=...]
    @PostMapping(value = "/{serverId}/rcon", params = "await=true")
    public de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket rconAwait(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid RconCommand command,
            @RequestParam(name = "timeoutMs", required = false) Long timeoutMs,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        var response = serverService.rconCommandAwait(server, command, timeoutMs);

        eventPublisher.publishEvent(new ServerRconCommandRequestEvent(user.getId(), server, command.getCommand()));
        return response;
    }

    @PostMapping("/{serverId}/reset-settings")
    public void resetSettings(
            @PathVariable @Identifier String serverId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.resetServerSettings(server);

        // For reset, existing code logs NODE_SETTINGS_CHANGE even though it targets server settings — keep as-is by describing via ServerSettingsChangedEvent
        eventPublisher.publishEvent(new ServerSettingsChangedEvent(user.getId(), server, "Reset server settings"));
    }

    @PostMapping("/{serverId}/wipe-savegames")
    public void wipeSavegames(
            @PathVariable @Identifier String serverId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.wipeSavegames(server);

        eventPublisher.publishEvent(new ServerSavegamesWipedEvent(user.getId(), server));
    }

    @Data
    public static class ChangeDisplayNameRequest {
        @SafeName
        private String displayName;
    }

    @PostMapping("/{serverId}/change-display-name")
    public void changeDisplayName(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid ChangeDisplayNameRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);

        String oldName = server.getDisplayName();
        serverService.changeDisplayName(server, request.getDisplayName());

        eventPublisher.publishEvent(new ServerSettingsChangedEvent(user.getId(), server, "Changed display name from \"" + oldName + "\" to \"" + request.getDisplayName() + "\""));
    }

    @Data
    public static class DeleteServerRequest {
        private boolean forceDelete;
    }

    @PostMapping("/{serverId}/delete")
    public void delete(
            @PathVariable @Identifier String serverId,
            @RequestBody @Valid DeleteServerRequest request,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissions.require(user, server, Permission.MANAGE_SERVERS);
        serverService.deleteServer(server, request.isForceDelete());

        eventPublisher.publishEvent(new ServerDeletedEvent(user.getId(), server));
    }

}
