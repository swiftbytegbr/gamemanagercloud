package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.controller.NodeController;
import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.node.NodeRegisteredEvent;
import de.swiftbyte.gmc.backend.exception.NodeException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.NodeTaskRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import de.swiftbyte.gmc.backend.util.Generators;
import de.swiftbyte.gmc.common.entity.GameServerDto;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import de.swiftbyte.gmc.common.model.NodeTask;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeDeletePacket;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeLoginAckPacket;
import de.swiftbyte.gmc.common.packet.from.bidirectional.node.NodeSettingsPacket;
import de.swiftbyte.gmc.common.packet.from.backend.node.NodeUpdatePacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeHeartbeatPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeLoginPacket;
import de.swiftbyte.gmc.common.packet.from.daemon.node.NodeLogoutPacket;
import de.swiftbyte.gmc.backend.service.node.NodeTaskService;
import lombok.extern.slf4j.Slf4j;
import de.swiftbyte.gmc.backend.mapper.GameServerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class NodeService {


    @Value("${gmc.backend.version}")
    private String backendVersion;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private Environment environment;

    @Autowired
    private StompSender stompSender;

    @Autowired
    private ServerService serverService;

    @Autowired
    private GameServerMapper gameServerMapper;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ServerRepository serverRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private LimitService limitService;
    @Autowired
    private NodeTaskService nodeTaskService;
    @Autowired
    private NodeTaskRepository nodeTaskRepository;


    public String getLatestVersion() {
        return adminService.getDaemonVersion();
    }

    public Node getOrThrow(String nodeId) {
        Node node = nodeRepository.findById(nodeId).orElseThrow(NotFoundException::new);
        checkStatus(node);
        return node;
    }

    public void checkStatus(Node node) {
        if (node.getStatus() == Node.Status.ONLINE && node.getLastAlive().isBefore(Instant.now().minusSeconds(30))) {
            NodeLogoutPacket packet = new NodeLogoutPacket();
            packet.setReason("timeout");
            logout(node, packet);
        }
    }

    @Async
    @Scheduled(fixedRate = 1000 * 60)
    protected void iterateAndCheckStatus() {
        // Target only nodes that have timed out instead of scanning all
        Instant cutoff = Instant.now().minusSeconds(30);
        List<Node> timedOut = nodeRepository.findByStatusAndLastAliveBefore(Node.Status.ONLINE, cutoff);
        if (timedOut.isEmpty()) return;
        NodeLogoutPacket packet = new NodeLogoutPacket();
        packet.setReason("timeout");
        timedOut.forEach(node -> logout(node, packet));
    }


    public List<Node> findByTeamId(String teamId) {
        List<Node> nodes = nodeRepository.findByTeamId(teamId);
        nodes.forEach(this::checkStatus);
        return nodes;
    }


    public Node create(String teamId, String name) {
        nodeRepository.deleteAll(nodeRepository.findByTeamIdAndStatus(teamId, Node.Status.CREATION));
        // Enforce max nodes per team (per-team limit if configured > 0)
        int existing = nodeRepository.findByTeamId(teamId).size();
        int maxNodes = limitService.getTeamLimit(teamId, LimitService.LimitType.NODES);
        if (maxNodes > 0 && existing >= maxNodes) {
            throw new TeamException("too_many_nodes").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        }
        nodeRepository.findByTeamId(teamId).forEach(node -> {
            if (node.getSettings().getName().equals(name)) throw new NodeException("name_already_exists").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        });

        Node node = new Node();
        node.setTeamId(teamId);
        node.setSecret(Generators.generateNodeSecret());
        node.setInviteToken(Generators.generateVerificationToken());
        node.setStatus(Node.Status.CREATION);

        NodeSettings settings = new NodeSettings();
        settings.setName(name);

        settings.setManageFirewallAutomatically(true);
        settings.setEnableAutoUpdate(true);

        node.setSettings(settings);

        return nodeRepository.save(node);
    }

    public NodeController.NodeRegisterResponse registerNode(int inviteToken, String defaultServerDirectory) {
        if (inviteToken == 0) throw new NodeException("invalid_invite_token").setHttpStatus(org.springframework.http.HttpStatus.BAD_REQUEST);
        Node node = nodeRepository.findByInviteToken(inviteToken).orElseThrow(() -> new NodeException("invalid_invite_token").setHttpStatus(org.springframework.http.HttpStatus.BAD_REQUEST));
        NodeController.NodeRegisterResponse response = new NodeController.NodeRegisterResponse();
        if (node.getStatus() != Node.Status.CREATION) throw new NodeException("invalid_state").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        response.setNodeId(node.getId());
        response.setNodeSecret(node.getSecret());
        node.setInviteToken(0);
        node.setStatus(Node.Status.OFFLINE);
        // If the daemon provided a default server directory, persist it to node settings
        if (defaultServerDirectory != null && !defaultServerDirectory.isBlank()) {
            if (node.getSettings() == null) node.setSettings(new NodeSettings());
            node.getSettings().setDefaultServerDirectory(defaultServerDirectory);
        }
        nodeRepository.save(node);

        // Emit event (no issuer context)
        eventPublisher.publishEvent(new NodeRegisteredEvent(node));


        return response;
    }

    public void delete(String nodeId) {
        Node node = getOrThrow(nodeId);
        serverRepository.findByNodeId(nodeId).forEach(server -> serverService.deleteServer(server, true));
        // Fail and remove all tasks belonging to this node
        nodeTaskService.failAllTasksForNode(node, "Node deleted");
        nodeRepository.delete(node);

        NodeDeletePacket packet = new NodeDeletePacket();
        stompSender.sendToNode(node, "/node/delete", packet);
    }


    public void changeSettings(Node node, NodeSettings nodeSettings) {
        if (node.getStatus() != Node.Status.ONLINE) throw new NodeException("invalid_state").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);

        if(!node.getSettings().getServerBackupsDirectory().equals(nodeSettings.getServerBackupsDirectory())) {
            boolean blocked = nodeTaskRepository.existsByNodeIdAndType(node.getId(), NodeTask.Type.BACKUP_DIRECTORY_CHANGE);
            if(blocked) throw new NodeException("backup_directory_change_in_progress").setHttpStatus(org.springframework.http.HttpStatus.CONFLICT);
        }
        node.setSettings(nodeSettings);
        nodeRepository.save(node);


        NodeSettingsPacket packet = new NodeSettingsPacket();
        packet.setNodeSettings(nodeSettings);
        stompSender.sendToNode(node, "/node/settings", packet);
        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);
        stompSender.sendToTeam(team, "/node/settings", packet);
    }



    public void sendUpdate(Node node, boolean isAutoUpdate) {
        NodeUpdatePacket packet = new NodeUpdatePacket();
        packet.setVersion(adminService.getDaemonVersion());
        packet.setAutoUpdate(isAutoUpdate);
        stompSender.sendToNode(node, "/node/update", packet);
    }


    // --- Stomp Routes ---

    public NodeLoginAckPacket login(Node node, NodeLoginPacket packet) {
        NodeLoginAckPacket response = new NodeLoginAckPacket();
        response.setBackendVersion(backendVersion);
        response.setBackendProfile(environment.getActiveProfiles()[0]);
        response.setNodeSettings(node.getSettings());

        response.setGameServers(serverRepository.findByNodeId(node.getId()).stream().map(this::mapToDaemonDto).toList());

        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);
        response.setTeamName(team.getName());

        node.setLastLogin(Instant.ofEpochSecond(packet.getTimestamp()));
        node.setLogoutReason(null);
        node.setStatus(Node.Status.ONLINE);
        node.setDaemonVersion(packet.getDaemonVersion());
        node.setLastAlive(Instant.ofEpochSecond(packet.getTimestamp()));
        node.setSystemData(packet.getNodeData());
        nodeRepository.save(node);

        stompSender.sendToTeam(team, "/node/login", packet);

        if (!packet.getDaemonVersion().equals(adminService.getDaemonVersion())) sendUpdate(node, true);
        return response;
    }

    private GameServerDto mapToDaemonDto(GameServer server) { return gameServerMapper.toCommonDto(server); }

    public void logout(Node node, NodeLogoutPacket packet) {
        node.setLogoutReason(packet.getReason());
        node.setStatus(Node.Status.OFFLINE);
        serverRepository.findByNodeId(node.getId()).forEach(server -> serverService.disconnect(server.getId()));
        // Fail and purge all tasks for this node, notifying the team
        nodeTaskService.failAllTasksForNode(node, "Node " + node.getId() + " disconnected: " + packet.getReason());
        nodeRepository.save(node);

        stompSender.sendToTeam(teamRepository.findById(node.getTeamId()).orElseThrow(), "/node/logout", packet);
    }

    /**
     * Applies node settings pushed by the daemon and notifies the team's users.
     */
    public void updateSettingsFromDaemon(Node node, NodeSettingsPacket packet) {
        if (packet == null || packet.getNodeSettings() == null) return;
        node.setSettings(packet.getNodeSettings());
        nodeRepository.save(node);

        Team team = teamRepository.findById(node.getTeamId()).orElseThrow(NotFoundException::new);
        stompSender.sendToTeam(team, "/node/settings", packet);
    }


    @Async
    public void heartbeat(Node node, NodeHeartbeatPacket packet) {
        if(node.getStatus().equals(Node.Status.OFFLINE)) reconnectTimeout(node);

        // Entire heartbeat processing runs asynchronously to free HTTP thread
        node.setStatus(Node.Status.ONLINE);
        node.setLastAlive(Instant.ofEpochSecond(packet.getTimestamp()));
        packet.getGameServers().forEach(serverService::heartbeat);
        Team team = teamRepository.findById(node.getTeamId()).orElseThrow();
        stompSender.sendToTeam(team, "/node/heartbeat", packet);
        nodeRepository.save(node);
    }

    private void reconnectTimeout(Node node) {
        node.setLastLogin(Instant.now());
        node.setLogoutReason(null);

        NodeLoginPacket packet = new NodeLoginPacket();
        packet.setDaemonVersion(node.getDaemonVersion());
        packet.setNodeData(node.getSystemData());
        stompSender.sendToTeam(teamRepository.findById(node.getTeamId()).orElseThrow(), "/node/login", packet);

        NodeSettingsPacket packet2 = new NodeSettingsPacket();
        packet2.setNodeSettings(node.getSettings());
        stompSender.sendToNode(node, "/node/settings", packet2);
    }


}
