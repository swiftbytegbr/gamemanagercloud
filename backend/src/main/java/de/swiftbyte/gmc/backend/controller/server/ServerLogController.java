package de.swiftbyte.gmc.backend.controller.server;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.model.server.ServerLogFile;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.ServerService;
import de.swiftbyte.gmc.backend.service.server.ServerLogService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
public class ServerLogController {

    @Autowired
    private PermissionService permissionService;
    @Autowired
    private ServerService serverService;
    @Autowired
    private ServerLogService serverLogService;

    @GetMapping("/server/{serverId}/logs")
    public List<ServerLogFile> list(
            @PathVariable @Identifier String serverId,
            @RequestParam(name = "includeContent", defaultValue = "false") boolean includeContent,
            @RequestParam(name = "latestOnly", defaultValue = "true") boolean latestOnly,
            @AuthenticationPrincipal User user
    ) {
        GameServer server = serverService.getOrThrow(serverId);
        permissionService.require(user, server, Permission.ACCESS_LOGS);
        return serverLogService.listByServer(serverId, includeContent, latestOnly);
    }

    @GetMapping("/server/{serverId}/logs/{logId}")
    public ServerLogFile get(
            @PathVariable @Identifier String serverId,
            @PathVariable String logId,
            @AuthenticationPrincipal User user) {
        GameServer server = serverService.getOrThrow(serverId);
        permissionService.require(user, server, Permission.ACCESS_LOGS);
        return serverLogService.getById(serverId, logId);
    }

    

    @DeleteMapping("/server/{serverId}/logs/{logId}")
    public void delete(
            @PathVariable @Identifier String serverId,
            @PathVariable String logId,
            @AuthenticationPrincipal User user
    ) {
        GameServer server = serverService.getOrThrow(serverId);
        permissionService.require(user, server, Permission.MANAGE_LOGS);
        serverLogService.deleteById(serverId, logId);
    }

}
