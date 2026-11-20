package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.common.model.NodeTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NodeTaskRepository extends MongoRepository<NodeTask, String> {

    List<NodeTask> findByNodeId(String nodeId);

    boolean existsByNodeIdAndTypeAndTargetIdsContainingAndStateIn(
            String nodeId,
            NodeTask.Type type,
            String targetId,
            List<NodeTask.State> states
    );

    boolean existsByNodeIdAndType(
            String nodeId,
            NodeTask.Type type);

    long deleteByNodeIdIn(java.util.List<String> nodeIds);
}
