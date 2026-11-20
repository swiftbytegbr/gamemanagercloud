package de.swiftbyte.gmc.backend.domain.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "images")
public class Image {
    @Id
    private String id;

    // MIME type like image/png, image/jpeg, etc.
    private String contentType;

    // Original format hint (png, jpg, webp, gif)
    private String format;

    // Raw image bytes as stored
    private byte[] data;

    private Instant createdAt;
}

