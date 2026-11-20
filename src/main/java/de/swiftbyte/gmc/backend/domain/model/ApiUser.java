package de.swiftbyte.gmc.backend.domain.model;

import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Team-scoped API user. Extends User but persists in a separate collection.
 */
@Document(collection = "api_user")
public class ApiUser extends User {
}

