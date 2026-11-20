package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.ActionLogItem;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.events.ActionLogEvent;
import de.swiftbyte.gmc.backend.events.ActionLogType;
import de.swiftbyte.gmc.backend.events.BaseEvent;
import de.swiftbyte.gmc.backend.exception.NodeException;
import de.swiftbyte.gmc.backend.exception.TeamException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.TeamActionLogItemPacket;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.repository.TeamActionLogRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TeamActionLogService {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamActionLogRepository teamActionLogRepository;

    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private StompSender stompSender;

    public void addActionLogItem(Team team, ActionLogItem item) {
        if (item.getTimestamp() == null) item.setTimestamp(Instant.now());
        item.setTeamId(team.getId());
        teamActionLogRepository.save(item);
        // Broadcast new item to team members
        TeamActionLogItemPacket packet = new TeamActionLogItemPacket();
        packet.setItem(item);
        stompSender.sendToTeam(team, "/team/action-log", packet);
        // No pruning: keep full action history per team
    }

    public void addActionLogItem(String teamId, ActionLogItem item) {
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new TeamException("not_found"));
        addActionLogItem(team, item);
    }

    public void addActionLogItemByNodeId(String nodeId, ActionLogItem item) {
        String teamId = nodeRepository.findById(nodeId).orElseThrow(() -> new NodeException("not_found")).getTeamId();
        addActionLogItem(teamId, item);
    }

    // === Single Event Listener using annotation + ActionLogEvent accessors ===
    @Async
    @EventListener
    public void onActionLogEvent(ActionLogEvent event) {
        // Allow events to opt-out of logging
        if (!event.isActionLogEnabled()) return;
        ActionLogType typeAnn = event.getClass().getAnnotation(ActionLogType.class);
        if (typeAnn == null) return;

        ActionLogItem item = new ActionLogItem(typeAnn.value());
        if (event instanceof BaseEvent base) {
            item.setIssuerId(base.getIssuerId());
            item.setTimestamp(base.getTimestamp());
        }
        // Prefer new actionLogInformation field; fallback to legacy accessor
        String info = event.getActionLogInformation();
        item.setActionInformation(info);
        item.setAffectedUserId(event.getAffectedUserId());

        if (event.getServer() != null) {
            var srv = event.getServer();
            item.setAffectedServerId(srv.getId());
            item.setAffectedNodeId(srv.getNodeId());
            addActionLogItemByNodeId(srv.getNodeId(), item);
            return;
        }
        if (event.getNode() != null) {
            var node = event.getNode();
            item.setAffectedNodeId(node.getId());
            addActionLogItem(node.getTeamId(), item);
            return;
        }
        if (event.getTeam() != null) {
            addActionLogItem(event.getTeam(), item);
        }
    }

    public void clearActionLog(Team team) {
        teamActionLogRepository.deleteByTeamId(team.getId());
    }

    public Page<ActionLogItem> getActionLog(String teamId, Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 100 : Math.max(1, size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "timestamp"));
        return teamActionLogRepository.findByTeamId(teamId, pageable);
    }

    // Pruning removed by request; action log retains all entries
}
