package de.swiftbyte.gmc.backend.controller.server;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.model.server.GameServerStatistic;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.server.ServerStatisticsService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@Validated
@ApiAccountAllowed
public class ServerStatisticsController {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private ServerStatisticsService statisticsService;


    @GetMapping("/{nodeId}/{serverId}/statistics")
    public GameServerStatistic getStatistics(@PathVariable @Identifier String nodeId, @PathVariable @Identifier String serverId, @AuthenticationPrincipal User user) {
        permissionService.requireByNodeId(user, nodeId, Permission.ACCESS_SERVERS);
        return statisticsService.getOrThrow(serverId);
    }
}
