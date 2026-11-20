package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SessionRepository extends MongoRepository<Session, String> {

    void deleteAllByUserId(String userId);

    List<Session> findByUserId(String userId);

    Page<Session> findByUserId(String userId, Pageable pageable);
}
