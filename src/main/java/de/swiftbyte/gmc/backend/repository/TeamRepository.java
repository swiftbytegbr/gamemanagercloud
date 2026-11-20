package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends MongoRepository<Team, String> {
    Optional<Team> findByInviteCode(String inviteCode);

    List<Team> findByOwnerId(String ownerID);

    @Query("{ 'members.?0': { $exists: true } }")
    List<Team> findByMembersContaining(String memberID);

    @Query("{ '$or': [ { 'ownerId': ?0 }, { 'members.?0': { $exists: true } }, { 'applications.?0': { $exists: true } } ] }")
    List<Team> findAllByUserIdIncludingApplications(String userId);


    boolean existsByName(String name);


}
