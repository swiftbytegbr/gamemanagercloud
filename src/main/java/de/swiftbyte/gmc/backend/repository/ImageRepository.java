package de.swiftbyte.gmc.backend.repository;

import de.swiftbyte.gmc.backend.domain.model.Image;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImageRepository extends MongoRepository<Image, String> {
}

