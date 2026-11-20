package de.swiftbyte.gmc.backend.service.admin;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.mapper.NodeMapper;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.service.NodeService;
import de.swiftbyte.gmc.common.entity.NodeSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class NodeAdminService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private NodeMapper nodeMapper;

    public Page<NodeDto> searchNodes(String q,
                                     String teamId,
                                     Node.Status status,
                                     String daemonVersion,
                                     Integer page,
                                     Integer size,
                                     String sortKey,
                                     Sort.Direction dir) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 50 : Math.max(1, size);
        Sort sort = Sort.by(dir == null ? Sort.Direction.ASC : dir, mapSortKey(sortKey));
        Pageable pageable = PageRequest.of(p, s, sort);

        Query query = new Query();
        List<Criteria> ands = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String regex = ".*" + Pattern.quote(q.trim()) + ".*";
            ands.add(new Criteria().orOperator(
                    Criteria.where("_id").regex(regex, "i"),
                    Criteria.where("settings.name").regex(regex, "i")
            ));
        }
        if (teamId != null && !teamId.isBlank()) ands.add(Criteria.where("teamId").is(teamId));
        if (status != null) ands.add(Criteria.where("status").is(status));
        if (daemonVersion != null && !daemonVersion.isBlank()) ands.add(Criteria.where("daemonVersion").is(daemonVersion));
        if (!ands.isEmpty()) query.addCriteria(new Criteria().andOperator(ands));

        long total = mongoTemplate.count(query, Node.class);
        query.with(pageable);
        List<Node> nodes = mongoTemplate.find(query, Node.class);
        return new PageImpl<>(nodes.stream().map(nodeMapper::toDto).toList(), pageable, total);
    }

    private String mapSortKey(String sortKey) {
        String key = (sortKey == null || sortKey.isBlank()) ? "settings.name" : sortKey;
        return switch (key) {
            case "name" -> "settings.name";
            case "lastAlive" -> "lastAlive";
            case "lastLogin" -> "lastLogin";
            case "status" -> "status";
            case "daemonVersion" -> "daemonVersion";
            default -> "settings.name";
        };
    }

    public NodeDto getNode(String nodeId) {
        return nodeRepository.findById(nodeId).map(nodeMapper::toDto).orElseThrow(NotFoundException::new);
    }

    public void updateSettings(String nodeId, NodeSettings settings) {
        Node node = nodeService.getOrThrow(nodeId);
        if (node.getStatus() == Node.Status.ONLINE) {
            nodeService.changeSettings(node, settings);
        } else {
            node.setSettings(settings);
            nodeRepository.save(node);
        }
    }

    public void updateName(String nodeId, String name) {
        Node node = nodeService.getOrThrow(nodeId);
        NodeSettings s = node.getSettings() == null ? new NodeSettings() : node.getSettings();
        s.setName(name);
        updateSettings(nodeId, s);
    }

    public void deleteNode(String nodeId) {
        nodeService.delete(nodeId);
    }

    public void sendUpdate(String nodeId, boolean auto) {
        Node node = nodeService.getOrThrow(nodeId);
        nodeService.sendUpdate(node, auto);
    }
}

