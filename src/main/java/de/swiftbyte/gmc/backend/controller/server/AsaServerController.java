package de.swiftbyte.gmc.backend.controller.server;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.ServerService;
import de.swiftbyte.gmc.backend.service.server.AsaServerService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@ApiAccountAllowed
public class AsaServerController {


    @Autowired
    private ServerService serverService;

    @Autowired
    private AsaServerService asaServerService;

    @GetMapping(value = "/server/{serverId}/ark/asa/adminlist.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getAdminlist(@PathVariable @Identifier String serverId) {
        GameServer server = serverService.getOrThrow(serverId);
        String body = asaServerService.renderAdminlist(server);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(body);
    }
}

