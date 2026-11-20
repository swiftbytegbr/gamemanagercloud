package de.swiftbyte.gmc.backend.config;

import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.beans.factory.InitializingBean;
import java.util.concurrent.TimeUnit;

@Configuration
public class MongoSessionExpiryConfig implements InitializingBean {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void afterPropertiesSet() {
        IndexOptions options = new IndexOptions().expireAfter(0L, TimeUnit.SECONDS);
        mongoTemplate.getCollection("sessions")
                .createIndex(new Document("expiresAt", 1), options);
    }
}
