package de.swiftbyte.gmc.backend.domain.model.config;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Accessors(chain = true)
@Document(collection = "config")
public class GmcConfig {

    @Id
    private String id;

    private Object value;
}

