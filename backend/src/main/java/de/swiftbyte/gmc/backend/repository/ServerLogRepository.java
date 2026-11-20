package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.server.ServerLogFile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ServerLogRepository extends MongoRepository<ServerLogFile, String> {
    List<ServerLogFile> findByServerIdOrderByUpdatedAtDesc(String serverId);
    List<ServerLogFile> findByServerIdOrderByCreatedAtDesc(String serverId);
    Optional<ServerLogFile> findByServerIdAndFilenameAndCreatedAt(String serverId, String filename, Instant createdAt);
    boolean existsByServerIdAndFilenameAndCreatedAt(String serverId, String filename, Instant createdAt);
    int countByServerId(String serverId);

    long deleteByServerIdAndFilenameAndCreatedAt(String serverId, String filename, Instant createdAt);
    long deleteByServerId(String serverId);

    Optional<ServerLogFile> findTopByServerIdOrderByUpdatedAtAsc(String serverId);

    @Query(value = "{ 'serverId': ?0 }", fields = "{ 'content': 0 }")
    List<ServerLogFile> findAllByServerIdExcludeContent(String serverId, Sort sort);

    @Aggregation(pipeline = {
            "{ $match: { serverId: ?0 } }",
            "{ $sort: { createdAt: -1 } }",
            "{ $group: { _id: '$filename', doc: { $first: '$$ROOT' } } }",
            "{ $replaceRoot: { newRoot: '$doc' } }",
            "{ $project: { content: 0 } }",
            "{ $sort: { createdAt: -1 } }"
    })
    List<ServerLogFile> findLatestPerFilenameExcludeContent(String serverId);

    @Aggregation(pipeline = {
            "{ $match: { serverId: ?0 } }",
            "{ $sort: { createdAt: -1 } }",
            "{ $group: { _id: '$filename', doc: { $first: '$$ROOT' } } }",
            "{ $replaceRoot: { newRoot: '$doc' } }",
            "{ $sort: { createdAt: -1 } }"
    })
    List<ServerLogFile> findLatestPerFilename(String serverId);

}
