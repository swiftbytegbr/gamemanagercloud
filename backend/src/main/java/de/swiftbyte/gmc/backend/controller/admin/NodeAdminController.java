package de.swiftbyte.gmc.backend.controller.admin;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.dto.PageDto;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.admin.NodeAdminService;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/nodes")
@Validated
@RequiredArgsConstructor
public class NodeAdminController {

    private final PermissionService permissions;
    private final NodeAdminService nodeAdminService;

    // === Search & retrieval ===
    @GetMapping
    public PageDto<NodeDto> search(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "teamId", required = false) String teamId,
            @RequestParam(value = "status", required = false) Node.Status status,
            @RequestParam(value = "daemonVersion", required = false) String daemonVersion,
            @RequestParam(value = "page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") @Min(1) @Max(200) Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(value = "dir", required = false, defaultValue = "ASC") Sort.Direction dir,
            @AuthenticationPrincipal User currentUser
    ) {
        permissions.requireGmcAdmin(currentUser);
        Page<NodeDto> res = nodeAdminService.searchNodes(q, teamId, status, daemonVersion, page, size, sort, dir);
        return PageDto.from(res);
    }

    @GetMapping("/{nodeId}")
    public NodeDto get(@PathVariable String nodeId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        return nodeAdminService.getNode(nodeId);
    }

    // === Editing ===
    @Data
    public static class ChangeNameRequest {
        @NotBlank
        private String name;
    }

    @PatchMapping("/{nodeId}/name")
    public void changeName(@PathVariable String nodeId, @RequestBody @Valid ChangeNameRequest req, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        nodeAdminService.updateName(nodeId, req.getName());
    }

    @PatchMapping("/{nodeId}/settings")
    public void updateSettings(@PathVariable String nodeId, @RequestBody @Valid NodeSettings settings, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        nodeAdminService.updateSettings(nodeId, settings);
    }

    @PostMapping("/{nodeId}/update")
    public void sendUpdate(@PathVariable String nodeId,
                           @RequestParam(value = "auto", required = false, defaultValue = "true") boolean auto,
                           @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        nodeAdminService.sendUpdate(nodeId, auto);
    }

    @DeleteMapping("/{nodeId}")
    public void deleteNode(@PathVariable String nodeId, @AuthenticationPrincipal User currentUser) {
        permissions.requireGmcAdmin(currentUser);
        nodeAdminService.deleteNode(nodeId);
    }
}

