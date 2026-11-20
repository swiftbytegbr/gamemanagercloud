package de.swiftbyte.gmc.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Discover and register available modules (e.g., JavaTimeModule)
        mapper.findAndRegisterModules();
        return mapper;
    }
}

