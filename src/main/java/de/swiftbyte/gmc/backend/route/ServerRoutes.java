package de.swiftbyte.gmc.backend.route;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.service.ServerService;
import de.swiftbyte.gmc.backend.service.server.ServerLogService;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerStatePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerLogChunkPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerVersionChangedPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerRconResponsePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerBackupResponsePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerDeleteBackupResponsePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerDeleteResponse;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerChangeDirectoryFailedPacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class ServerRoutes {

    @Autowired
    private ServerService serverService;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private ServerLogService serverLogService;


    // --- Server Routes ---
    @MessageMapping("/server/state")
    public void serverStateUpdate(@Payload ServerStatePacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.stateUpdate(packet);
    }

    @MessageMapping("/server/version")
    public void serverVersionChange(@Payload ServerVersionChangedPacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.versionChangedPacket(packet);
    }


    @MessageMapping("/server/rcon")
    public void serverRconUpdate(@Payload ServerRconResponsePacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.rconResponse(packet);

    }


    @MessageMapping("/server/backup")
    public void serverBackupUpdate(@Payload ServerBackupResponsePacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.backupResponse(packet);
    }

    @MessageMapping("/server/delete-backup")
    public void serverBackupDelete(@Payload ServerDeleteBackupResponsePacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.backupDeleteResponse(packet);
    }

    @MessageMapping("/server/delete")
    public void serverDelete(@Payload ServerDeleteResponse packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.deleteResponse(packet);
    }

    @MessageMapping("/server/change-directory-failed")
    public void serverChangeDirectoryFailed(@Payload ServerChangeDirectoryFailedPacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverService.changeDirectoryFailed(packet);
    }

    // Legacy write endpoint removed; chunk-based logging only

    @MessageMapping("/server/log-chunk")
    public void serverLogChunk(@Payload ServerLogChunkPacket packet, Authentication authentication) {
        if (isForbidden((Node) authentication.getPrincipal(), packet.getServerId())) return;
        serverLogService.writeChunkFromDaemon(packet);
    }


    private boolean isForbidden(Node node, String serverId) {
        Optional<GameServer> serverOptional = serverRepository.findById(serverId);
        if (serverOptional.isEmpty()) return true;
        return !serverOptional.get().getNodeId().equals(node.getId());
    }

}
