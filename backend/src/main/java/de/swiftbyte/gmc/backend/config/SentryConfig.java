package de.swiftbyte.gmc.backend.config;

import io.sentry.Sentry;
import io.sentry.SentryClient;
import io.sentry.SentryUncaughtExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SentryConfig {


    @Bean
    public SentryClient initSentryClient(@Value("${sentry.dsn:}") String dsn) {
        if (dsn == null || dsn.isBlank()) {
            log.warn("SENTRY_DSN is not set, Sentry will be disabled.");
            return null;
        }
        Sentry.init(dsn);
        SentryClient sentryClient = Sentry.getStoredClient();
        // Capture all uncaught exceptions, including background threads
        SentryUncaughtExceptionHandler.setup();
        log.info("Sentry initialized successfully.");
        return sentryClient;
    }
}
