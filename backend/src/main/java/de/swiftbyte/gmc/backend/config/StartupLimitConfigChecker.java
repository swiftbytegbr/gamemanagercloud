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
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Ensures the default limits ({@code config/defaultLimits}) exist and applies limits configured in the environment.
 * <p>
 * On the first start the document is created from the built-in defaults ({@link AdminService#defaultLimits()}).
 * Every limit that is set in the environment (GMC_LIMIT_*, see application.properties) overrides the stored value on
 * each start; limits that are not set keep their stored value, so they can still be changed via the admin API.
 * A value of 0 means unlimited.
 */
@Slf4j
@Configuration
public class StartupLimitConfigChecker {

    @Autowired
    private ConfigRepository configRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private Environment environment;

    // Property (mapped to a GMC_LIMIT_* variable in application.properties) -> setter on LimitsConfig
    private static final Map<String, BiConsumer<LimitsConfig, Integer>> PROPERTIES = new LinkedHashMap<>();

    static {
        PROPERTIES.put("gmc.limits.user-max-owned-teams", (c, v) -> c.userMaxOwnedTeams = v);
        PROPERTIES.put("gmc.limits.team-max-members", (c, v) -> c.teamMaxMembers = v);
        PROPERTIES.put("gmc.limits.team-max-nodes", (c, v) -> c.teamMaxNodes = v);
        PROPERTIES.put("gmc.limits.team-max-servers", (c, v) -> c.teamMaxServers = v);
        PROPERTIES.put("gmc.limits.server-max-backups", (c, v) -> c.serverMaxBackups = v);
        PROPERTIES.put("gmc.limits.server-max-rcon-commands", (c, v) -> c.serverMaxRconCommands = v);
        PROPERTIES.put("gmc.limits.server-statistics-max-size", (c, v) -> c.serverStatisticsMaxSize = v);
        PROPERTIES.put("gmc.limits.server-max-logs", (c, v) -> c.serverMaxLogs = v);
    }

    @Bean
    public ApplicationRunner defaultLimitsStartupCheck() {
        return args -> {
            boolean exists = configRepository.findById("defaultLimits").isPresent();
            LimitsConfig limits = exists ? adminService.getLimitsConfig() : adminService.defaultLimits();

            List<String> overridden = new ArrayList<>();
            PROPERTIES.forEach((property, setter) -> {
                Integer value = readLimit(property);
                if (value == null) return;
                setter.accept(limits, value);
                overridden.add(property.substring("gmc.limits.".length()) + "=" + value);
            });

            if (!exists || !overridden.isEmpty()) {
                configRepository.save(new GmcConfig().setId("defaultLimits").setValue(limits));
            }
            if (!exists) log.info("Created default limits.");
            if (!overridden.isEmpty()) log.info("Default limits from environment: {}", String.join(", ", overridden));
            log.info("Default limits (0 = unlimited): owned teams per user {}, members {}, nodes {}, servers {} per team; "
                            + "backups {}, RCON commands {}, statistics entries {}, logs {} per server.",
                    limits.userMaxOwnedTeams, limits.teamMaxMembers, limits.teamMaxNodes, limits.teamMaxServers,
                    limits.serverMaxBackups, limits.serverMaxRconCommands, limits.serverStatisticsMaxSize, limits.serverMaxLogs);
            if (limits.serverStatisticsMaxSize == 0) {
                log.warn("Statistics are unlimited: the statistics document of a server grows without bound and can exceed the "
                        + "MongoDB document size limit. Set GMC_LIMIT_SERVER_STATISTICS_MAX_SIZE.");
            }
        };
    }

    /** Returns the configured limit, or null if it is not set (empty). Invalid values stop the startup. */
    private Integer readLimit(String property) {
        String raw = environment.getProperty(property);
        if (raw == null || raw.isBlank()) return null;
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) throw new NumberFormatException();
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid value '" + raw + "' for " + property + ": expected a number >= 0 (0 = unlimited)");
        }
    }
}
