package de.swiftbyte.gmc.backend.domain.model.server;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "server_logs")
@CompoundIndexes({
        @CompoundIndex(name = "uniq_server_filename_created", def = "{ 'serverId': 1, 'filename': 1, 'createdAt': 1 }", unique = true),
        @CompoundIndex(name = "idx_server_updated", def = "{ 'serverId': 1, 'updatedAt': -1 }")
})
public class ServerLogFile {
    @Id
    private String id;

    @Indexed
    private String serverId;

    @Indexed
    private String teamId;

    private String filename;

    private Instant createdAt;

    @Indexed(expireAfter = "7d")
    private Instant updatedAt;

    private int sizeBytes;

    private String content;
}
