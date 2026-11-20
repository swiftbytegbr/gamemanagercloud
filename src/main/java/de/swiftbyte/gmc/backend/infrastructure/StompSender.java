package de.swiftbyte.gmc.backend.infrastructure;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StompSender {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NodeRepository nodeRepository;


    public void send(String destination, Object payload) {
        destination = "/topic" + destination;
        messagingTemplate.convertAndSend(destination, payload);
    }

    public void sendToUser(User user, String destination, Object payload) {
        destination = "/queue" + destination;
        messagingTemplate.convertAndSendToUser(user.getId(), destination, payload);
    }

    @Async
    public void sendToTeam(Team team, String destination, Object payload) {
        // delegate to exclusion-aware variant with no exclusions
        sendToTeam(team, destination, payload, Set.of());
    }

    /**
     * Sends a message to all members (and owner) of a team, excluding provided userIds.
     */
    @Async
    public void sendToTeam(Team team, String destination, Object payload, Collection<String> excludeUserIds) {
        destination = "/queue" + destination;
        List<String> userIds;
        if (team.getMembers() == null) userIds = new ArrayList<>();
        else userIds = new ArrayList<>(team.getMembers().keySet());
        userIds.add(team.getOwnerId());

        Set<String> exclude = excludeUserIds == null ? Set.of() : excludeUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (String userId : userIds) {
            if (userId == null || exclude.contains(userId)) continue;
            messagingTemplate.convertAndSendToUser(userId, destination, payload);
        }
    }

    public void sendToNode(Node node, String destination, Object payload) {
        sendToNode(node.getId(), destination, payload);
    }

    public void sendToNode(String nodeId, String destination, Object payload) {
        destination = "/queue" + destination;
        messagingTemplate.convertAndSendToUser(nodeId, destination, payload);
    }

    public void sendToNodesByTeam(Team team, String destination, Object payload) {
        nodeRepository.findByTeamId(team.getId()).forEach(node -> sendToNode(node, destination, payload));
    }

}
