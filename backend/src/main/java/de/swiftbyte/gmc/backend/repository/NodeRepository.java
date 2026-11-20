package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.Node;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;

public interface NodeRepository extends MongoRepository<Node, String> {

    Optional<Node> findByInviteToken(int inviteToken);

    // void deleteNodesWhereStatusIs(Node.Status status);

    boolean existsNodeBySettings_Name(String name);

    List<Node> findByTeamId(String teamId);
    Page<Node> findByTeamId(String teamId, Pageable pageable);
    long deleteByTeamId(String teamId);

    List<Node> findByTeamIdAndStatus(String teamId, Node.Status status);

    // Targeted query for timed-out nodes
    List<Node> findByStatusAndLastAliveBefore(Node.Status status, Instant cutoff);
}
