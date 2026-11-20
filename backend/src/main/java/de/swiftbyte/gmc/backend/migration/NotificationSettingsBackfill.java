package de.swiftbyte.gmc.backend.migration;

import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
@Configuration
public class NotificationSettingsBackfill {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public ApplicationRunner backfillNotificationThreshold() {
        return args -> {
            try {
                // Set threshold to ERROR where missing
                Query q = new Query(new Criteria().orOperator(
                        Criteria.where("notificationSettings").exists(false),
                        Criteria.where("notificationSettings.emailNotificationsMinSeverity").exists(false)
                ));
                Update u = new Update().set("notificationSettings.emailNotificationsMinSeverity", NotificationSeverity.ERROR);
                var res = mongoTemplate.updateMulti(q, u, User.class);
                long modified = res.getModifiedCount();
                if (modified > 0) {
                    log.info("Backfilled notification threshold to ERROR for {} users.", modified);
                } else {
                    log.info("Notification threshold backfill: no changes needed.");
                }

                // Set emailNotificationsEnabled = false where missing
                Query q2 = new Query(new Criteria().orOperator(
                        Criteria.where("notificationSettings").exists(false),
                        Criteria.where("notificationSettings.emailNotificationsEnabled").exists(false)
                ));
                Update u2 = new Update().set("notificationSettings.emailNotificationsEnabled", false);
                var res2 = mongoTemplate.updateMulti(q2, u2, User.class);
                long modified2 = res2.getModifiedCount();
                if (modified2 > 0) {
                    log.info("Backfilled emailNotificationsEnabled=false for {} users.", modified2);
                } else {
                    log.info("Email notifications enabled flag backfill: no changes needed.");
                }
            } catch (Exception ex) {
                log.warn("Notification threshold backfill failed: {}", ex.getMessage());
            }
        };
    }
}
