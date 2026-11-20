package de.swiftbyte.gmc.backend.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.Date;

@Data
@Document("sessions")
public class Session {

    @Id
    private String sessionId;
    @Indexed
    private String userId;

    private Instant created;
    private Instant lastAccessed;
    private String lastIp;
    private String lastUserAgent;

    @Field("expiresAt")
    private Date expiresAt;
}
