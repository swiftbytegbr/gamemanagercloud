package de.swiftbyte.gmc.backend.service.node;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.exception.NodeException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.NodeTaskRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.common.model.NodeTask;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeTaskCancelPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeTaskCompletePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeTaskCreatePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeTaskUpdatePacket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import de.swiftbyte.gmc.backend.service.NotificationService;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.common.model.NotificationSource;

@Service
public class NodeTaskService {

    @Autowired
    private NodeTaskRepository nodeTaskRepository;

    @Autowired
    private StompSender stompSender;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private NotificationService notificationService;

    public List<NodeTask> findByNodeId(String nodeId) {
        return nodeTaskRepository.findByNodeId(nodeId);
    }

    public Optional<NodeTask> findById(String taskId) {
        return nodeTaskRepository.findById(taskId);
    }

    public NodeTask getOrThrow(String taskId) {
        return nodeTaskRepository.findById(taskId).orElseThrow(NotFoundException::new);
    }


    public void deleteNodeTask(NodeTask task) {
        nodeTaskRepository.delete(task);
    }

    public void requestNodeTaskCancellation(String taskId, Node node) {
        NodeTaskCancelPacket packet = new NodeTaskCancelPacket();
        NodeTask task = getOrThrow(taskId);
        if(!task.isCancellable()) throw new NodeException(("task_not_cancelable")).setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        packet.setNodeTaskId(taskId);
        stompSender.sendToNode(node, "/node/task-cancel", packet);
    }

    // --------------- STOMP Packets ---------------

    public NodeTask createNodeTask(NodeTaskCreatePacket packet, Node node) {
        NodeTask task = packet.getNodeTask();
        // Prevent overwriting an existing task when node provides an existing ID
        if (task.getId() != null && nodeTaskRepository.findById(task.getId()).isPresent()) {
            throw new NodeException("task_id_already_exists").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        }
        task.setNodeId(node.getId());
        task = nodeTaskRepository.save(task);
        stompSender.sendToTeam(teamRepository.findById(node.getTeamId()).orElseThrow(), "/node/task-create", packet);
        return task;
    }

    public void updateNodeTask(NodeTaskUpdatePacket packet, Node node) {
        NodeTask task = packet.getNodeTask();
        // Ensure the task exists and belongs to the same node
        NodeTask existing = nodeTaskRepository.findById(task.getId()).orElseThrow(NotFoundException::new);
        if (!node.getId().equals(existing.getNodeId())) {
            throw new NodeException("task_node_mismatch").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        }
        task.setNodeId(node.getId());

        // Avoid upsert to prevent resurrecting a task that was completed/deleted concurrently
        Query q = Query.query(Criteria.where("_id").is(task.getId()));
        FindAndReplaceOptions opts = FindAndReplaceOptions.options().returnNew();
        NodeTask replaced = mongoTemplate.findAndReplace(q, task, opts, "node_tasks");
        if (replaced != null) {
            stompSender.sendToTeam(teamRepository.findById(node.getTeamId()).orElseThrow(), "/node/task-update", packet);
            // Create notification if transitioned to FAILED
            if (task.getState() == NodeTask.State.FAILED && existing.getState() != NodeTask.State.FAILED) {
                createTaskFailedNotification(node, task, task.getErrorMessage(), NotificationSource.DAEMON);
            }
        }
    }

    public void completeNodeTask(NodeTaskCompletePacket packet, Node node) {
        NodeTask task = packet.getNodeTask();
        if (task == null || task.getId() == null) {
            throw new NodeException("invalid_task_id").setHttpStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
        }

        NodeTask existing = nodeTaskRepository.findById(task.getId()).orElseThrow(NotFoundException::new);
        if (!node.getId().equals(existing.getNodeId())) {
            throw new NodeException("task_node_mismatch").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        }

        // Optionally set terminal metadata here (e.g., finishedAt) if needed by consumers
        nodeTaskRepository.deleteById(existing.getId());

        NodeTaskCompletePacket notifyPacket = new NodeTaskCompletePacket();
        notifyPacket.setNodeTask(packet.getNodeTask());
        stompSender.sendToTeam(teamRepository.findById(node.getTeamId()).orElseThrow(), "/node/task-complete", notifyPacket);

        // If final state is FAILED, create a notification; otherwise notify completion
        if (task.getState() == NodeTask.State.FAILED) {
            createTaskFailedNotification(node, task, task.getErrorMessage(), NotificationSource.DAEMON);
        } else {
            createTaskCompletedNotification(node, task, NotificationSource.DAEMON);
        }
    }


    /**
     * Mark all tasks of a node as FAILED, notify the team, and remove them.
     */
    public void failAllTasksForNode(Node node, String reason) {
        List<NodeTask> tasks = nodeTaskRepository.findByNodeId(node.getId());
        if (tasks.isEmpty()) return;

        var team = teamRepository.findById(node.getTeamId()).orElseThrow();
        for (NodeTask task : tasks) {
            task.setState(NodeTask.State.FAILED);
            task.setErrorMessage(reason != null ? reason : "Node disconnected");
            task.setFinishedAt(Instant.now());

            NodeTaskCompletePacket completePacket = new NodeTaskCompletePacket();
            completePacket.setNodeTask(task);
            nodeTaskRepository.deleteById(task.getId());
            stompSender.sendToTeam(team, "/node/task-complete", completePacket);

            // Notify team about failure (source BACKEND since backend marks them failed)
            createTaskFailedNotification(node, task, task.getErrorMessage(), NotificationSource.BACKEND);
        }
    }

    private void createTaskFailedNotification(Node node, NodeTask task, String reason, NotificationSource source) {
        String title = "Task failed";
        StringBuilder msg = new StringBuilder();
        msg.append("Task ").append(task.getType()).append(" failed on node ").append(node.getSettings().getName()).append(".");
        if (reason != null && !reason.isBlank()) {
            msg.append("\nReason: ").append(reason);
        }
        notificationService.create(node.getTeamId(),
                "TASK_FAILED",
                NotificationSeverity.ERROR,
                title,
                msg.toString(),
                source);
    }

    private void createTaskCompletedNotification(Node node, NodeTask task, NotificationSource source) {
        String title = "Task completed";
        StringBuilder msg = new StringBuilder();
        msg.append("Task ").append(task.getType()).append(" completed on node ").append(node.getSettings().getName()).append(".");
        notificationService.create(
                node.getTeamId(),
                "TASK_COMPLETED",
                NotificationSeverity.INFO,
                title,
                msg.toString(),
                source
        );
    }




}
