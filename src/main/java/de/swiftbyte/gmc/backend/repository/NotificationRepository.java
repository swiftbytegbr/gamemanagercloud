package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByTeamIdOrderByCreatedAtDesc(String teamId, Pageable pageable);

    List<Notification> findByTeamIdOrderByCreatedAtDesc(String teamId);

    @Query(value = "{ 'teamId': ?0, '$or': [ { 'readByIds': { '$exists': false } }, { 'readByIds': { '$nin': [?1] } } ] }", count = true)
    long countUnread(String teamId, String userId);

    long deleteByTeamId(String teamId);
}
