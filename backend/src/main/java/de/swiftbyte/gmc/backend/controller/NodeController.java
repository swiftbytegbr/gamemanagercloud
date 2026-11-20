package de.swiftbyte.gmc.backend.controller;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.events.node.NodeCreatedEvent;
import de.swiftbyte.gmc.backend.events.node.NodeDeletedEvent;
import de.swiftbyte.gmc.backend.events.node.NodeSettingsChangedEvent;
import de.swiftbyte.gmc.backend.events.node.NodeUpdateRequestEvent;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.NodeService;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.validation.Identifier;
import de.swiftbyte.gmc.backend.validation.SafeName;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import de.swiftbyte.gmc.backend.mapper.NodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/node")
@Validated
public class NodeController {

    @Autowired
    NodeService nodeService;

    @Autowired
    NodeMapper nodeMapper;

    @Autowired
    PermissionService permissions;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    // --- Create ---

    @Data
    public static class CreateNodeRequest {
        @Identifier
        private String teamId;
        @SafeName
        private String name;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public NodeDto createNode(@RequestBody @Valid CreateNodeRequest request, @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, request.getTeamId(), Permission.MANAGE_NODES);

        Node node = nodeService.create(request.getTeamId(), request.getName());
        NodeDto nodeDto = nodeMapper.toDto(node);

        // Publish creation after success
        eventPublisher.publishEvent(new NodeCreatedEvent(user.getId(), node));
        return nodeDto;
    }

    @Data
    public static class RegisterNodeRequest {
        @Min(1)
        private int inviteToken;
        // Absolute path to the node's default server directory
        private String defaultServerDirectory;
    }

    @Data
    public static class NodeRegisterResponse {
        private String nodeId;
        private String nodeSecret;
    }

    @PostMapping("/register")
    public NodeRegisterResponse registerNode(@RequestBody RegisterNodeRequest request) {
        return nodeService.registerNode(request.getInviteToken(), request.getDefaultServerDirectory());
    }

    // --- Get latest Version number ---
    @ApiAccountAllowed
    @GetMapping(value = "/latest-version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getLatestVersion() {
        return nodeService.getLatestVersion();
    }

    // --- Utils ---

    @ApiAccountAllowed
    @GetMapping("/{nodeId}")
    public NodeDto getNode(@PathVariable @Identifier String nodeId, @AuthenticationPrincipal User user) {
        Node node = nodeService.getOrThrow(nodeId);
        permissions.require(user, node, Permission.ACCESS_NODES);
        return nodeMapper.toDto(node);
    }

    @ApiAccountAllowed
    @GetMapping("/by-team/{teamId}")
    public List<NodeDto> getByTeamId(@PathVariable @Identifier String teamId, @AuthenticationPrincipal User user) {
        permissions.requireByTeamId(user, teamId, Permission.ACCESS_NODES);
        return nodeService.findByTeamId(teamId).stream().map(nodeMapper::toDto).toList();
    }

    @ApiAccountAllowed
    @DeleteMapping("/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNode(@PathVariable @Identifier String nodeId, @AuthenticationPrincipal User user) {
        permissions.requireByNodeId(user, nodeId, Permission.MANAGE_NODES);
        Node node = nodeService.getOrThrow(nodeId);
        nodeService.delete(nodeId);
        eventPublisher.publishEvent(new NodeDeletedEvent(user.getId(), node));
    }

    // --- Settings ---

    @ApiAccountAllowed
    @PostMapping("/{nodeId}/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeSettings(@PathVariable @Identifier String nodeId,
                               @RequestBody @Valid NodeSettingsUpdateRequest request,
                               @AuthenticationPrincipal User user) {
        Node node = nodeService.getOrThrow(nodeId);
        permissions.require(user, node, Permission.MANAGE_NODES);
        NodeSettings nodeSettings = nodeMapper.toNodeSettings(request);
        nodeService.changeSettings(node, nodeSettings);
        eventPublisher.publishEvent(new NodeSettingsChangedEvent(user.getId(), node));
    }

    @Data
    public static class NodeSettingsUpdateRequest {
        private String name;
        private String description;
        //@ServerDirectoryPath TODO
        private String defaultServerDirectory;
        private String serverBackupsDirectory;
        @Deprecated
        private String serverPath;
        private boolean enableAutoUpdate;
        private boolean manageFirewallAutomatically;
    }

    @ApiAccountAllowed
    @PostMapping("/{nodeId}/update")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateNode(@PathVariable @Identifier String nodeId, @AuthenticationPrincipal User user) {
        Node node = nodeService.getOrThrow(nodeId);
        permissions.require(user, node, Permission.MANAGE_NODES);
        nodeService.sendUpdate(node, false);
        eventPublisher.publishEvent(new NodeUpdateRequestEvent(user.getId(), node));
    }

}
