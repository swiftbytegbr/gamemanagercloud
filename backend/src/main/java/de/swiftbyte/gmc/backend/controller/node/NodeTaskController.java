package de.swiftbyte.gmc.backend.controller.node;

import de.swiftbyte.gmc.backend.domain.enums.Permission;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.security.ApiAccountAllowed;
import de.swiftbyte.gmc.backend.service.NodeService;
import de.swiftbyte.gmc.backend.service.PermissionService;
import de.swiftbyte.gmc.backend.service.node.NodeTaskService;
import de.swiftbyte.gmc.common.model.NodeTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/node/{nodeId}/task")
@ApiAccountAllowed
public class NodeTaskController {

    @Autowired
    NodeTaskService nodeTaskService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private PermissionService permissionService;

    @GetMapping
    public List<NodeTask> getNodeTasks(@PathVariable String nodeId, @AuthenticationPrincipal User user) {
        permissionService.requireByNodeId(user, nodeId, Permission.ACCESS_NODES);
        return nodeTaskService.findByNodeId(nodeId);
    }

    @GetMapping("/{taskId}")
    public NodeTask getNodeTask(@PathVariable String nodeId, @PathVariable String taskId, @AuthenticationPrincipal User user) {
        permissionService.requireByNodeId(user, nodeId, Permission.ACCESS_NODES);
        return nodeTaskService.getOrThrow(taskId);
    }

    @PostMapping("/{taskId}/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void cancelNodeTask(@PathVariable String nodeId, @PathVariable String taskId, @AuthenticationPrincipal User user) {
        permissionService.requireByNodeId(user, nodeId, Permission.MANAGE_NODES);
        nodeTaskService.requestNodeTaskCancellation(taskId, nodeService.getOrThrow(nodeId));
    }


}
