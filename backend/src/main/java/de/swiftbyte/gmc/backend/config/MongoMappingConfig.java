package de.swiftbyte.gmc.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

@Configuration
public class MongoMappingConfig {

    public MongoMappingConfig(MappingMongoConverter converter) {
        converter.setMapKeyDotReplacement(".");
    }


}
