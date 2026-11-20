package de.swiftbyte.gmc.backend.aop;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.exception.NodeException;
import de.swiftbyte.gmc.backend.exception.ServerException;
import de.swiftbyte.gmc.backend.repository.NodeTaskRepository;
import de.swiftbyte.gmc.common.model.NodeTask;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Aspect
@Component
public class TaskBlockAspect {

    @Autowired
    private NodeTaskRepository nodeTaskRepository;

    @Around("@annotation(annotation)")
    public Object aroundServerMethods(ProceedingJoinPoint pjp, BlockOnNodeTasksForServer annotation) throws Throwable {
        GameServer server = findArg(pjp.getArgs(), GameServer.class);
        if (server == null) return pjp.proceed();

        String nodeId = server.getNodeId();
        String serverId = server.getId();

        List<NodeTask> tasks = nodeTaskRepository.findByNodeId(nodeId);
        boolean blockAll = annotation.all();
        List<NodeTask.Type> types = Arrays.asList(annotation.types());

        for (NodeTask t : tasks) {
            if (!blockAll && !types.contains(t.getType())) continue;
            // Node-global tasks have null/empty targets and must block all servers on the node
            boolean isNodeGlobal = t.getTargetIds() == null || t.getTargetIds().isEmpty();
            boolean targetsServer = !isNodeGlobal && t.getTargetIds().contains(serverId);
            if (isNodeGlobal || targetsServer) {
                throw mapServerException(t.getType());
            }
        }

        return pjp.proceed();
    }

    @Around("@annotation(annotation)")
    public Object aroundNodeMethods(ProceedingJoinPoint pjp, BlockOnNodeTasksForNode annotation) throws Throwable {
        Node node = findArg(pjp.getArgs(), Node.class);
        if (node == null) return pjp.proceed();

        List<NodeTask> tasks = nodeTaskRepository.findByNodeId(node.getId());
        boolean blockAll = annotation.all();
        List<NodeTask.Type> types = Arrays.asList(annotation.types());

        for (NodeTask t : tasks) {
            if (!blockAll && !types.contains(t.getType())) continue;
            throw mapNodeException(t.getType());
        }

        return pjp.proceed();
    }

    private <T> T findArg(Object[] args, Class<T> type) {
        if (args == null) return null;
        for (Object arg : args) {
            if (type.isInstance(arg)) return type.cast(arg);
        }
        return null;
    }

    private de.swiftbyte.gmc.backend.exception.GmcException mapServerException(NodeTask.Type type) {
        String key;
        switch (type) {
            case SERVER_DIRECTORY_CHANGE -> key = "server_directory_change_in_progress";
            case BACKUP_DIRECTORY_CHANGE -> key = "backup_directory_change_in_progress";
            case TIMED_SHUTDOWN -> key = "timed_shutdown_already_scheduled";
            case TIMED_RESTART -> key = "timed_restart_already_scheduled";
            default -> key = "task_in_progress";
        }
        return new ServerException(key).setHttpStatus(HttpStatus.CONFLICT);
    }

    private de.swiftbyte.gmc.backend.exception.GmcException mapNodeException(NodeTask.Type type) {
        String key;
        switch (type) {
            case BACKUP_DIRECTORY_CHANGE -> key = "backup_directory_change_in_progress";
            default -> key = "task_in_progress";
        }
        return new NodeException(key).setHttpStatus(HttpStatus.CONFLICT);
    }
}
