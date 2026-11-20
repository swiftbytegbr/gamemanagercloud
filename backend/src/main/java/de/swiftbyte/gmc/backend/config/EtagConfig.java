package de.swiftbyte.gmc.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class EtagConfig {
    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        // Lets Spring generate ETag for responses based on content
        return new ShallowEtagHeaderFilter();
    }
}

