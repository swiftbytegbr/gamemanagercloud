package de.swiftbyte.gmc.backend.route;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.service.NodeService;
import de.swiftbyte.gmc.backend.service.node.NodeTaskService;
import de.swiftbyte.gmc.common.model.NodeTask;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeLoginAckPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.*;
import de.swiftbyte.gmc.common.packet.from.bidirectional.node.NodeSettingsPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class NodeRoutes {


    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeTaskService nodeTaskService;

    @MessageMapping("/node/login")
    @SendToUser("/queue/node/login")
    public NodeLoginAckPacket nodeLogin(@Payload NodeLoginPacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        return nodeService.login(node, packet);
    }

    @MessageMapping("/node/logout")
    public void nodeLogout(@Payload NodeLogoutPacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        nodeService.logout(node, packet);
    }


    @MessageMapping("/node/heartbeat")
    public void nodeHeartbeat(@Payload NodeHeartbeatPacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        nodeService.heartbeat(node, packet);
    }

    @MessageMapping("/node/task-create")
    @SendToUser("/queue/node/task-create")
    public NodeTask nodeTaskCreate(@Payload NodeTaskCreatePacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        return nodeTaskService.createNodeTask(packet, node);
    }

    @MessageMapping("/node/task-update")
    public void nodeTaskUpdate(@Payload NodeTaskUpdatePacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        nodeTaskService.updateNodeTask(packet, node);
    }

    @MessageMapping("/node/task-complete")
    public void nodeTaskComplete(@Payload NodeTaskCompletePacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        nodeTaskService.completeNodeTask(packet, node);
    }

    @MessageMapping("/node/settings")
    public void nodeSettingsChanged(@Payload NodeSettingsPacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        nodeService.updateSettingsFromDaemon(node, packet);
    }


}
