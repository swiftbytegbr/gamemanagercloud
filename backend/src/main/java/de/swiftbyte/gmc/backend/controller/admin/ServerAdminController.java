package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.PageDto;
import de.swiftbyte.gmc.backend.dto.ServerAdminDto;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.admin.ServerAdminService;
import de.swiftbyte.gmc.common.entity.GameServerState;
import de.swiftbyte.gmc.common.entity.GameType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/admin/servers")
@Validated
@RequiredArgsConstructor
public class ServerAdminController {

    private final PermissionService permissions;
    private final ServerAdminService serverAdminService;

    @GetMapping
    public PageDto<ServerAdminDto> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "teamId", required = false) String teamId,
            @RequestParam(value = "nodeId", required = false) String nodeId,
            @RequestParam(value = "gameType", required = false) GameType gameType,
            @RequestParam(value = "state", required = false) GameServerState state,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") @Min(1) @Max(200) Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "displayName") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "ASC") Sort.Direction dir,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<ServerAdminDto> res = serverAdminService.searchServers(q, teamId, nodeId, gameType, state, page, size, sort, dir);
        return PageDto.from(res);
    }

    @GetMapping("/{serverId}")
    public ServerAdminDto get(@PathVariable String serverId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        return serverAdminService.get(serverId);
    }
}

