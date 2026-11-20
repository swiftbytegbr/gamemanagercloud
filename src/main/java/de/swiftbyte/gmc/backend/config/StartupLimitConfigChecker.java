package de.swiftbyte.gmc.backend.config;

import de.swiftbyte.gmc.backend.domain.model.config.GmcConfig;
import de.swiftbyte.gmc.backend.domain.value.LimitsConfig;
import de.swiftbyte.gmc.backend.repository.ConfigRepository;
import de.swiftbyte.gmc.backend.service.admin.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Slf4j
@Configuration
public class StartupLimitConfigChecker {

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private AdminService adminService;

    @Bean
    public ApplicationRunner defaultLimitsStartupCheck() {
        return args -> {
            boolean created = false;

            // Ensure the defaultLimits document exists; if not, create with zeros and warn.
            if (configRepository.findById("defaultLimits").isEmpty()) {
                LimitsConfig defaults = adminService.defaultLimits();
                configRepository.save(new GmcConfig().setId("defaultLimits").setValue(defaults));
                created = true;
                log.warn("Default limits not found in config database. Backfilled all limits to 0. Please configure admin defaults.");
            }

            // Load current limits (either existing or just-created) and report any zero values.
            LimitsConfig limits = adminService.getLimitsConfig();

            boolean anyZero = false;
            anyZero |= warnIfZero("teamMaxNodes", limits.teamMaxNodes);
            anyZero |= warnIfZero("teamMaxServers", limits.teamMaxServers);
            anyZero |= warnIfZero("serverMaxBackups", limits.serverMaxBackups);
            anyZero |= warnIfZero("serverMaxRconCommands", limits.serverMaxRconCommands);
            anyZero |= warnIfZero("serverStatisticsMaxSize", limits.serverStatisticsMaxSize);
            anyZero |= warnIfZero("teamMaxMembers", limits.teamMaxMembers);
            anyZero |= warnIfZero("userMaxOwnedTeams", limits.userMaxOwnedTeams);

            if (!anyZero && !created) {
                log.info("Default limits check successful: all values are configured (> 0).");
            } else if (!created && anyZero) {
                log.warn("Default limits present but contain zero values (treated as unlimited). Consider configuring explicit limits.");
            } else if (created) {
                // We already logged a warn above about backfilling. Add a success acknowledgement of the check itself.
                log.info("Default limits startup check completed with backfill.");
            }
        };
    }

    private boolean warnIfZero(String name, int value) {
        if (Objects.equals(value, 0)) {
            log.warn("Default limit '{}' is 0 (unlimited).", name);
            return true;
        }
        return false;
    }
}
