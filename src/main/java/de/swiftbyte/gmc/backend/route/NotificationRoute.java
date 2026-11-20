package de.swiftbyte.gmc.backend.route;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.service.NodeService;
import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.common.model.NotificationSource;
import de.swiftbyte.gmc.common.packet.from.daemon.team.NotificationCreatePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationRoute {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NodeService nodeService;

    @MessageMapping("/notification/create")
    public void createFromDaemon(@Payload NotificationCreatePacket packet, Authentication authentication) {
        Node node = nodeService.getOrThrow(authentication.getName());
        notificationService.create(
            node.getTeamId(),
            packet.getType(),
            packet.getSeverity(),
            packet.getTitle(),
            packet.getMessage(),
            NotificationSource.DAEMON
        );
    }
}
