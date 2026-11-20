package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.ApiUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ApiUserRepository extends MongoRepository<ApiUser, String> {
    Optional<ApiUser> findByEmail(String email);
    boolean existsByEmail(String email);
}

